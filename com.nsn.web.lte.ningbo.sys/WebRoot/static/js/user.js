N.User = N.Class.extend({
	loadData:function(){
		
		$('#page_table').DataTable({
			"destroy": true,
		    "processing": true,
		    "serverSide": true,
		    "bFilter": false, // 过滤功能
		    "iDisplayLength":20,
		    "order":[[0,"asc"]],
		    "bLengthChange": false, // 改变每页显示数据数量
		    "ajax":  {
		        "url": "/sys/user/getPage",
		        "data": function(d){
		        	return $.extend({}, d, N.Util.serializeJson('searchForm'));
		        }
		    },
		    "columns": [
		        {"data": "id"},
		        {"data": "account"},
		        {"data": "name"},
		        {data : "status",
                 render : function(data, type, row) {
                     if (data == 1) { return "启用"; } 
                     else { return "禁用"; }
                 },
                },
		        {"data": "type"},
		        {"data": "email"},
		        {"data": "mobile"},
		        {"data": "remark"},
		        {render : function(data, type, row) {
		        	var editBtn = '<a href="javascript:user.editDialog(\''+row.id+'\');" class="btn btn-primary btn-xs btn-edit">编辑</a>&nbsp;&nbsp;';
		        	var relateBtn = '<a href="javascript:user.relateDialog(\''+row.id+'\');" class="btn btn-default btn-xs btn-del">角色</a>&nbsp;&nbsp;';
		        	var resetPwdBtn = '<a href="javascript:user.resetPwd(\''+row.id+'\');" class="btn btn-default btn-xs btn-info">重置密码</a>&nbsp;&nbsp;';
		        	var delBtn = '<a href="javascript:user.del(\''+row.id+'\');" class="btn btn-danger btn-xs btn-del">删除</a>'
                    return editBtn + relateBtn + resetPwdBtn +delBtn;
                  }
                }
		    ]
		});
	},
	addDialog:function(){
		var me = this;
		N.Util.clearForm('userForm');
		$("#op").val("add");
		$("#account").attr("disabled",false);
		$("#password").val("000000").attr("disabled",false);
		$("input[name='status']:eq(0)").prop("checked",'checked'); 
		$("input[name='sex']:eq(0)").prop("checked",'checked');
		$("#skin").find("option[value='blue']").prop("selected",true);
		this.selectCity();//选择城市的树
		layer.open({
            type: 1,shift: 2,
            shadeClose: true, //开启遮罩关闭
            title: '添加用户',
            content: $('#userDlg'),
            area: ['60%', '75%'],
            btn: ['提交','取消'],
    		yes: function(){me.save()}
        });
	},
    editDialog:function(id){
    	var me = this;
    	layer.load(1);
    	$.ajax({
			type: "POST",
			url:"/sys/user/getUser",
			data:{"id":id},// id
			dataType:'json',
		    success: function(data) {
		    	layer.closeAll('loading');
		    	N.Util.loadForm('userForm',data);
		    	$("#op").val("edit");
		    	$("#account").attr("disabled",true);
		    	$("#password").val("000000").attr("disabled",true);
		    	me.selectCity();//选择城市的树
	    		layer.open({
	                type: 1,shift: 2,
	                shadeClose: true, //开启遮罩关闭
	                title: '修改用户',
	                content: $('#userDlg'),
	                area: ['60%', '75%'],
	                btn: ['提交','取消'],
	        		yes: function(){me.save()}
	            });
		    }
		});
    },
    /*
     * 校验
     */
    vali:function(form){
    	var flag = true;
    	//4到20位数字和26个英文字母
    	var num_letter_reg = /^[A-Za-z0-9]{4,20}$/;
    	//正整数
    	var num_reg = /^[0-9]*$/;
    	
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
    			if(!num_reg.test(v)){ msg="请输入数字"; }
    		}else if(vali=='numletter'){
    			if(!num_letter_reg.test(v)){ msg="请输入4~20位字母或数字"; }
    		}
    		if(msg!=""){
    			layer.tips(msg, '#'+$(this).attr("id"), {
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
			url:'/sys/user/save',
			data:$('#userForm').serialize(),// 你的formid
			dataType:'json',
		    success: function(data) {
		    	layer.closeAll('loading');
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
        layer.confirm('是否删除？', {icon: 3, title:'提示'}, function(){
        	layer.load(1);
        	$.ajax({
            	type: "POST",
   				url:"/sys/user/delete",
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
   			    		N.Util.reloadData('page_table');
   			    	}
   			    }
   			});
    	});
    },
    resetPwd:function(id){
        layer.confirm('是否重置？', {icon: 3, title:'提示'}, function(){
        	layer.load(1);
        	$.ajax({
            	type: "POST",
   				url:"/sys/user/resetpwd",
   				data:{"id":id},// id
   				dataType:'json',
   			    error: function(request) {
   			    	layer.closeAll('loading');
   			    	layer.msg('操作失败');
   			    },
   			    success: function(data) {
   			    	layer.closeAll('loading');
   			    	layer.msg(data.msg);
   			    }
   			});
    	});
    },
    /*
     * 关联角色弹窗
     */
    relateDialog:function(id){
    	var me = this;
    	$("#chos").empty();//清空列表
        $("#unchos").empty();//清空列表
    	layer.load(1);
    	$.ajax({
			type: "POST",
			url:"/sys/user/getRelateList",
			data:{"id":id},// id
			dataType:'json',
		    error: function(request) {
		    	layer.msg('操作失败');
		    },
		    success: function(data) {
		    	var relate = data.relate;
		    	var unRelate = data.unRelate;
		    	$(relate).each(function (index, obj) {
		    		$("#chos").append('<li data-rid="'+obj.rid+'" onclick="user.relateListClick(this)">'+obj.name+'</li>');
	            });
		    	$(unRelate).each(function (index, obj) {
		    		$("#unchos").append('<li data-rid="'+obj.rid+'" onclick="user.relateListClick(this)">'+obj.name+'</li>');
	            });
		    	$("#relate_uid").val(id);//role_id
		    	layer.closeAll('loading');
		    	layer.open({
	                type: 1,shift: 2,
	                shadeClose: true, //开启遮罩关闭
	                title: '关联角色',
	                content: $('#relateDlg'),
	                area: ['50%', '70%'],
	                btn: ['提交','取消'],
	        		yes: function(){me.relate()}
	            });
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
        layer.load(1);
        var uid = $("#relate_uid").val();//role_id
        var rids = "";//role_ids
        $("#chos").find("li").each(function(){
        	rids+=$(this).data("rid")+",";
        });
        if(rids.length>0){
        	rids=rids.substring(0,rids.length-1)
        }
        $.ajax({
        	type: "POST",
			url:"/sys/user/relate",
			data:{"rids":rids,"uid":uid},
			async: false,
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
		    		layer.msg('操作成功');
		    		layer.closeAll('page'); //关闭所有页面层
		    	}
		    }
		});
    },
    
    //城市下拉框
    selectCity:function(){
    	this.hideCity();//确保关毕城市下拉框
    	$.ajax({
			type: "POST",
			url:"/sys/user/getCity",
			dataType:'json',
		    success: function(data) {
		    	var setting = {//ztree设置
		    		check: {enable: true},//复选框树
	    			view: {
	    				txtSelectedEnable: false,
	    				showIcon: false
	    			},
                    data:{
                    	simpleData: {
        					enable: true,
        					idKey: "city_id",
        					rootPId: 0
        				},
		    			key: {
		    				name: "city_cn"
		    			}
                    },  
                    callback: {
                    	beforeCheck: function(treeId, treeNode) {
        					var check = (treeNode && !treeNode.isParent);
        					if (!check) layer.msg("只能选择城市");
        					return check;
        				},
        				onClick: function (e, treeId, treeNode, clickFlag) { 
        					var zTree = $.fn.zTree.getZTreeObj("cityTree");
        					zTree.checkNode(treeNode, !treeNode.checked, false, true); //不联动、触发回调
        				},
        				onCheck: function(e, treeId, treeNode) {
        					var zTree = $.fn.zTree.getZTreeObj("cityTree"),
        					nodes = zTree.getCheckedNodes(true),
        					city_cns = "", city_ids = "";
        					nodes.sort(function compare(a,b){return a.city_id-b.city_id;});
        					for (var i=0, l=nodes.length; i<l; i++) {
        						city_ids += nodes[i].city_id + ",";
        						city_cns += nodes[i].city_cn + ",";
        					}
        					if (city_ids.length > 0 ) city_ids = city_ids.substring(0, city_ids.length-1);
        					if (city_cns.length > 0 ) city_cns = city_cns.substring(0, city_cns.length-1);
        					$("#city").val(city_ids);
        					$("#citySel").val(city_cns);
        				}
        			}
                };  
	    		var cityTreeObj = $.fn.zTree.init($("#cityTree"), setting, data);
	    		//初始化树的选中状态以及页面输入框值
	    		var selected_codes = $("#city").val().split(",");
	    		$.each(selected_codes, function(index, obj) {//obj是city_i
		    		var treeNode = cityTreeObj.getNodeByParam("city_id", obj, null);  
		    		if(treeNode!=null){
		    			cityTreeObj.checkNode(treeNode, true, false, true); //不联动、触发回调
		    		}
		    	});
		    }
		});
    },
    showCity: function() {
		$("#menuContent").slideDown("fast");
		$("#userDlg").bind("mousedown", this.onBodyDown);
	},
	hideCity: function() {
		$("#menuContent").fadeOut("fast");
		$("#userDlg").unbind("mousedown", this.onBodyDown);
	},
	onBodyDown: function(event) {
		if (!(event.target.id == "menuBtn" || event.target.id == "menuContent" || $(event.target).parents("#menuContent").length>0)) {
			user.hideCity();
		}
	},
	
	exp: function(){
		//layer.msg("开始导出，请稍等...");
		N.Util.download("/sys/user/exp");
	}
});

var user = new N.User();

$(function(){
	user.loadData();
});
