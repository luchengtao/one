N.Role = N.Class.extend({
	loadData:function(){
		var me = this;
		$('#page_table').DataTable({
			"destroy": true,
		    "processing": true,
		    "serverSide": true,
		    "bFilter": false, // 过滤功能
		    "iDisplayLength":20,
		    "bLengthChange": false, // 改变每页显示数据数量
		    //"ajax": "/sys/role/getPage",
		    "ajax":  {
		        "url": "/sys/role/getPage",
		        "data": function(d){
		        	return $.extend({}, d, N.Util.serializeJson('searchForm'));
		        }
		    },
		    "columns" : [
            	{ data : "id", "visible": false},
                { data : "name" },
                { data : "enabled",
                  render : function(data, type, row) {
                      if (data == 1) { return "启用";} 
                      else { return "禁用"; }
                  }
                },
                { data : "remark",
                	render : function(data, type, row) {
                		var typeNames = ""; //特殊类型code对应的名称
                		if(data!=null && data!=""){
                			var typeCodes =data.split(",");
                			$.each(typeCodes, function(index, obj) {
                				$.each(me.typeTreeData, function(i,leaf) {
                					if(obj==leaf.code){
                						if(typeNames!=""){
                							typeNames+=","+leaf.name;
                						}else{
                							typeNames=leaf.name;
                						}
                					}
                				});
                        	});
                		}
                		return typeNames;
                    }
                },
                { render : function(data, type, row) {
                	var editBtn = '<a href="javascript:role.editDialog(\''+row.id+'\');" class="btn btn-primary btn-xs btn-edit">编辑</a>&nbsp;&nbsp;';
                	var relateBtn = '<a href="javascript:role.relateDialog(\''+row.id+'\');" class="btn btn-default btn-xs btn-del">用户</a>&nbsp;&nbsp;';
                	var menuBtn = '<a href="javascript:role.authDialog(\''+row.id+'\');" class="btn btn-default btn-xs btn-info">菜单</a>&nbsp;&nbsp;';
                	var delBtn = '<a href="javascript:role.del(\''+row.id+'\');" class="btn btn-danger btn-xs btn-del">删除</a>';
                    return editBtn + relateBtn + menuBtn + delBtn;
                  }
                }
            ]
		});
	},
	addDialog:function(){
		var me = this;
		N.Util.clearForm('roleForm');
		me.selectRemark();//选择特殊类别（如：脱敏）
		layer.open({
            type: 1,shift: 2,
            shadeClose: true, //开启遮罩关闭
            title: '添加角色',
            content: $('#roleDlg'),
            area: ['40%', '65%'],
            btn: ['提交','取消'],
    		yes: function(){me.save()}
        });
		$("#op").val("add");
	},
	editDialog:function(id){
		var me = this;
		layer.load(1);
    	$.ajax({
			type: "POST",
			url:"/sys/role/getRole",
			data:{"id":id},// id
			dataType:'json',
		    success: function(data) {
		    	layer.open({
		            type: 1,shift: 2,
		            shadeClose: true, //开启遮罩关闭
		            title: '修改角色',
		            content: $('#roleDlg'),
		            area: ['40%', '65%'],
		            btn: ['提交','取消'],
		    		yes: function(){me.save()}
		        });
		    	N.Util.loadForm('roleForm',data);
		    	me.selectRemark();//选择特殊类别（如：脱敏）
		    	$("#op").val("edit");
		    	layer.closeAll('loading');
		    }
		});
    },
    /*
     * 校验
     */
    vali:function(form){
    	var flag = true;
    	//数字和26个英文字母
    	var num_letter_reg = /^[A-Za-z0-9]+$/;
    	//正整数
    	var num_reg = /^[0-9]+$/;
    	
    	$('[data-vali]').each(function(){
    		var vali = $(this).data("vali");//校验类型
    		var v;
    		if($(this).attr("type")=='radio'){
    			v = $('input:radio[name="'+$(this).attr("name")+'"]:checked').val();
    		}else{
    			v = $(this).val();
    		}
    		var msg = "";
    		if(vali=='required'){
    			if(v==""||v==undefined){ msg="必填" }
    		}else if(vali=='onlynum'){
    			if(!num_reg.test(v)){ msg="限数字，且必填"; }
    		}else if(vali=='numletter'){
    			if(!num_letter_reg.test(v)){ msg="限字母数字，且必填"; }
    		}
    		if(msg!=""){
    			layer.tips(msg, '#'+$(this).attr("name"), {
        			tipsMore: true
        		});
    			flag = false;
    		}
    	});
    	return flag;
    },
	save:function(){
		if(!this.vali("userForm")){ return false; }
		//var me = this;
        layer.load(1);
        $.ajax({
			type: "POST",
			url:'/sys/role/save',
			data:$('#roleForm').serialize(),// 你的formid
			dataType:'json',
		    success: function(data) {
		    	layer.closeAll('loading');//注意这会关闭所有弹出
		    	layer.msg(data.msg);
		    	if(data.flag=='succ'){
		    		//me.loadData();
		    		N.Util.reloadData('page_table');
		    		layer.closeAll('page'); //关闭所有页面层
		    	}
		    }
		});
    },
    del:function(id){
    	//var me = this;
        layer.confirm('是否删除？', {icon: 3, title:'提示'}, function(){
        	layer.load(1);
        	$.ajax({
            	type: "POST",
   				url:"/sys/role/delete",
   				data:{"id":id},// id
   				dataType:'json',
   			    error: function(request) {
   			    	layer.closeAll('loading');
   			    	layer.msg('操作失败');
   			    },
   			    success: function(data) {
   			    	layer.closeAll('loading');
   			    	if(data.flag!="succ"){
   			    		layer.msg('操作失败');
   			    	}else{
   			    		layer.msg('删除成功');
   			    		//me.loadData();
   			    		N.Util.reloadData('page_table');
   			    	}
   			    }
   			});
    	});
    },
    /*
     * 关联用户弹窗
     */
    relateDialog:function(id){
    	var me = this;
    	$("#chos").empty();//清空列表
        $("#unchos").empty();//清空列表
        layer.load(1);
    	$.ajax({
			type: "POST",
			url:"/sys/role/getRelateList",
			data:{"id":id},// id
			dataType:'json',
		    success: function(data) {
		    	var relate = data.relate;
		    	var unRelate = data.unRelate;
		    	$(relate).each(function (index, obj) {
		    		$("#chos").append('<li data-uid="'+obj.uid+'" onclick="role.relateListClick(this)">'+obj.account+'</li>');
	            });
		    	$(unRelate).each(function (index, obj) {
		    		$("#unchos").append('<li data-uid="'+obj.uid+'" onclick="role.relateListClick(this)">'+obj.account+'</li>');
	            });
	        	$("#relate_rid").val(id);//role_id
	        	layer.open({
		            type: 1,
		            shift: 2,
		            shadeClose: true, //开启遮罩关闭
		            title: '关联用户',
		            content: $('#relateDlg'),
		            area: ['50%', '70%'],
		            btn: ['提交','取消'],
		    		yes: function(){me.relate()}
		        });
	    		layer.closeAll('loading');
		    }
		});
    },
    /*
     * 关联列表点击事件
     */
    relateListClick:function(obj){
    	//取消其他li的高亮
    	$(".list-ul li").removeClass("act");
    	//高亮选中的li
    	$(obj).addClass("act");
    },
    /*
     * 选择关联
     */
    relateChoose:function(op){
    	//选定的userId
    	var li = $(".act");
    	//获取父元素判断是已选还是未选
    	var type = $(li).parent().data("type");
    	console.log("op:"+op);
    	console.log("type:"+type);
    	if(op == 'chos' && type=='unchos'){
    		//未选变已选
    		$(li).appendTo("#chos"); 
    	}else if(op == 'unchos' && type=='chos'){
    		//已选变未选
    		$(li).appendTo("#unchos"); 
    	}
    },
    /*
     * 提交关联
     */
    relate:function(){
        console.log("===>relate()");
        layer.load(1);
        var rid = $("#relate_rid").val();//role_id
        var uids = "";//user_ids
        $("#chos").find("li").each(function(){
        	uids+=$(this).data("uid")+",";
        });
        if(uids.length>0){
        	uids=uids.substring(0,uids.length-1)
        }
        $.ajax({
        	type: "POST",
			url:"/sys/role/relate",
			data:{"uids":uids,"rid":rid},
			dataType:'json',
			async: false,
		    error: function(request) {
		    	layer.closeAll('loading');
		    	layer.msg('操作失败');
		    },
		    success: function(data) {
		    	layer.closeAll('loading');
		    	if(data.flag!="succ"){
		    		layer.msg('操作失败');
		    	}else{
		    		layer.msg('操作成功');
		    		layer.closeAll('page'); //关闭所有页面层
		    	}
		    }
		});
    },
    /*
     * 关联菜单
     */
    authDialog:function(id){
    	var me = this;
    	layer.load(1);
    	$.ajax({
			type: "POST",
			url:"/sys/role/menu",
			dataType:'json',
		    success: function(data) {
		    	var setting = {//ztree设置
                    data:{
                    	key: {children: "childs"}//子节点名
                    },  
                    check: {enable: true}//复选框树
                };  
	            var treeObj = $.fn.zTree.init($("#menuTree"),setting,data);  
	            treeObj.expandAll(true);
	            //选中已有权限
	            $.ajax({
	    			type: "POST",
	    			url:"/sys/role/getRoleMenu",
	    			data:{"id":id},// role_id
	    			dataType:'json',
	    		    success: function(result) {
	    		    	$.each(result, function(index, obj) {
	    		    		var permURI = obj.module;//拥有的权限
	    		    		//if(permURI.indexOf("/")==0 || permURI.indexOf("http://")==0 || permURI.indexOf("https://")==0){
	    		    			var treenode = treeObj.getNodeByParam("id", permURI, null);  
	    		    			if(treenode!=null && !treenode.isParent){
	    		    				treeObj.checkNode(treenode, true, true);//联动。注意，此处只处理子节点。
	    		    			}
	    		    		//}
	    		    	});
	    		    	layer.closeAll('loading');
	    		    	$("#auth_rid").val(id);//role_id
	    		    	layer.open({
	    		            type: 1,
	    		            shift: 2,
	    		            shadeClose: true, //开启遮罩关闭
	    		            title: '菜单',
	    		            content: $('#authDlg'),
	    		            area: ['20%', '60%'],
	    		            btn: ['提交','取消'],
	    		    		yes: function(){me.auth()}
	    		        });
	    		    }
	    		});
		    }
		});
    },
    /*
     * 提交授权
     */
    auth:function(){
    	layer.load(1);
    	var rid = $("#auth_rid").val();//role_id
    	var treeObj = $.fn.zTree.getZTreeObj("menuTree"),
        nodes = treeObj.getCheckedNodes(true),
        mids="";
        for(var i=0; i<nodes.length; i++){
        	if(!nodes[i].isParent){//只保存末端的权限节点，不保存任何父级节点
        		mids += nodes[i].id + ",";
        	}
        }
        if(mids.length>0){
        	mids=mids.substring(0,mids.length-1)
        }
        $.ajax({
        	type: "POST",
			url:"/sys/role/auth",
			data:{"mids":mids,"rid":rid},
			dataType:'json',
		    success: function(data) {
		    	layer.closeAll('loading');
		    	if(data.flag!="succ"){
		    		layer.msg('操作失败');
		    	}else{
		    		layer.msg('操作成功');
		    		layer.closeAll('page'); //关闭所有页面层
		    	}
		    }
		});
    },
    
    switchType:function(){
    	var flag = $("#type").val();
    	if(flag==1){//特殊角色
    		$("#remarkDiv").show();
    	}else{
    		$("#remarkDiv").hide();
    	}
    },
    
	//特殊权限下拉框
    typeTreeData : [//在此数组里添加供选择的特殊类别
	    {"name":"不脱敏","code":"sensitive"}
	],
    selectRemark:function(){
    	this.hideRemark();//确保关毕城市下拉框
    	this.switchType();
    	
    	var setting = {//ztree设置
    		check: {enable: true},//复选框树
			view: {
				txtSelectedEnable: false,
				showIcon: false
			},
            data:{
            	simpleData: {
					enable: true,
					idKey: "code",
					rootPId: 0
				},
    			key: {
    				name: "name"
    			}
            },  
            callback: {
            	beforeCheck: function(treeId, treeNode) {
					var check = (treeNode && !treeNode.isParent);
					if (!check) layer.msg("请选择特殊类别");
					return check;
				},
				onClick: function (e, treeId, treeNode, clickFlag) { 
					var zTree = $.fn.zTree.getZTreeObj("remarkTree");
					zTree.checkNode(treeNode, !treeNode.checked, false, true); //不联动、触发回调
				},
				onCheck: function(e, treeId, treeNode) {
					var zTree = $.fn.zTree.getZTreeObj("remarkTree"),
					nodes = zTree.getCheckedNodes(true),
					names = "", codes = "";
					nodes.sort(function compare(a,b){return a.code-b.code;});
					for (var i=0, l=nodes.length; i<l; i++) {
						codes += nodes[i].code + ",";
						names += nodes[i].name + ",";
					}
					if (codes.length > 0 ) codes = codes.substring(0, codes.length-1);
					if (names.length > 0 ) names = names.substring(0, names.length-1);
					$("#remark").val(codes);
					$("#remarkSel").val(names);
				}
			}
        };  
		var remarkTreeObj = $.fn.zTree.init($("#remarkTree"), setting, this.typeTreeData);
		//初始化树的选中状态以及页面输入框值
		var selected_codes = $("#remark").val().split(",");
		$.each(selected_codes, function(index, obj) {
    		var treeNode = remarkTreeObj.getNodeByParam("code", obj, null);  
    		if(treeNode!=null){
    			remarkTreeObj.checkNode(treeNode, true, false, true); //不联动、触发回调
    		}
    	});
    },
    showRemark: function() {
		$("#menuContent").slideDown("fast");
		$("#roleDlg").bind("mousedown", this.onBodyDown);
	},
	hideRemark: function() {
		$("#menuContent").fadeOut("fast");
		$("#roleDlg").unbind("mousedown", this.onBodyDown);
	},
	onBodyDown: function(event) {
		if (!(event.target.id == "menuBtn" || event.target.id == "menuContent" || $(event.target).parents("#menuContent").length>0)) {
			role.hideRemark();
		}
	}
});

var role = new N.Role();

$(function(){
	role.loadData();
});

