N.Menu = N.Class.extend({
	defaultMenuJson: function(){
		$("#default_menu_json").val("");
		$.get("/sys/menu/defaultMenu", function(data){
			//格式化输出
			var strJsonMenu = JSON.stringify(data, null, 4);
			console.log("当前菜单json：");
			console.log(strJsonMenu);
			$("#default_menu_json").val(strJsonMenu);
		},"json");
	},
	
	defaultMenuTree:{},
	defaultMenu:function(){
    	var me = this;
    	$.ajax({
			type: "GET",
			url:"/sys/menu/defaultMenu",
			dataType:'json',
		    success: function(data) {
		    	var setting = {//ztree设置
                    data:{
                    	key: {children: "childs"}//子节点名
                    },
                    view: {
	    				showIcon: false
	    			},
                    edit: {
        				enable: true,
        				drag:{
        					isCopy: true,
        					isMove: false,
        					prev: false,
        					inner: false,
        					next: false
        				},
        				showRemoveBtn: false,
        				showRenameBtn: false
        			},
        			callback: {
        				beforeDrag: Menu.beforeDrag,
        				beforeDrop: Menu.beforeDrop
        			}
                };  
	            me.defaultMenuTree = $.fn.zTree.init($("#default_menu_tree"),setting,data);  
	            me.defaultMenuTree.expandAll(true);
		    }
		});
    },
    
    customMenuTree:{},
    customMenu:function(){
    	var me = this;
    	var me = this;
    	$.ajax({
			type: "GET",
			url:"/sys/menu/customMenu",
			dataType:'json',
		    success: function(data) {
		    	var setting = {//ztree设置
	    			data:{
	    				key: {children: "childs"}//子节点名
	    			},
	    			view: {
	    				showIcon: false
	    			},
	    			edit: {
	    				enable: true,
	    				showRemoveBtn: false,
	    				showRenameBtn: false
	    			},
	    			callback: {
	    				beforeDrag: Menu.beforeDrag,
	    				beforeDrop: Menu.beforeDrop,
	    				onClick: Menu.onClick,
	    				onRename: Menu.onRename
	    			}
                };  
		    	me.customMenuTree = $.fn.zTree.init($("#custom_menu_tree"),setting,data);  
	            me.customMenuTree.expandAll(true);
		    }
		});
    },
    
    beforeDrag:function(treeId, treeNodes) {
		for (var i=0,l=treeNodes.length; i<l; i++) {
			if (treeNodes[i].drag === false) {
				return false;
			}
		}
		return true;
	},
    
	beforeDrop:function(treeId, treeNodes, targetNode, moveType) {
		var flag = targetNode ? targetNode.drop !== false : true;
		if(moveType=="inner" && targetNode && targetNode.id.indexOf("/")==0){
			flag = false;
			layer.msg("该节点不能创建子节点");
		}
		return flag;
	},
	
	onClick: function(event, treeId, treeNode) {
		$("#menu_name").val(treeNode.name);
		$("#menu_id").val(treeNode.id);
		$("#menu_icon").val(treeNode.icon);
	},
	
	onRename:function(event, treeId, treeNode, isCancel) {
		$("#menu_name").val(treeNode.name);
		$("#menu_id").val(treeNode.id);
		$("#menu_icon").val(treeNode.icon);
	},
	
	add:function(e) {
		var nodes = this.customMenuTree.getSelectedNodes();
		var treeNode = nodes[0];
		if (nodes.length == 0) {
			layer.msg("请先选择一个节点");
			return;
		}
		if(treeNode){
			if(treeNode.id.indexOf("/")==0){
				layer.msg("只能创建父级关系菜单");
			}else{
				var timestamp = (new Date()).valueOf()
				treeNode = this.customMenuTree.addNodes(treeNode, {
					id:"ID"+timestamp,
					name:"新建菜单"+timestamp
				});
				this.customMenuTree.editName(treeNode[0]);
			}
		}
	},
	
	edit: function(){
		var nodes = this.customMenuTree.getSelectedNodes();
		var treeNode = nodes[0];
		if (nodes.length == 0) {
			layer.msg("请先选择一个节点");
			return;
		}
		treeNode.id = $("#menu_id").val();
		treeNode.name = $("#menu_name").val();
		treeNode.icon = $("#menu_icon").val();
		this.customMenuTree.updateNode(treeNode);
	},
	
	remove: function(e) {
		var nodes = this.customMenuTree.getSelectedNodes();
		var treeNode = nodes[0];
		if (nodes.length == 0) {
			layer.msg("请先选择一个节点");
			return;
		}
		this.customMenuTree.removeNode(treeNode, false);
	},
	
	saveJson: function(){
		var strJson = this.customMenuTree.getNodes();
		/*
		N.Util.download("/sys/menu/exportMenuJson",{
			"menu":JSON.stringify(strJson[0])
		});
		*/
		layer.msg('开始导出，稍候弹出下载...请不要重复点击');
		var inputs = "<input type='hidden' name='menu' value='" + JSON.stringify(strJson[0]) + "' />";
        // request发送请求
        var $form = $('<form action="/sys/menu/exportMenuJson" method="post">' + inputs + '</form>').appendTo('body');
        $form.submit().remove();
	}
});

var Menu = new N.Menu();

$(function(){
	
	Menu.defaultMenu();
	Menu.customMenu();
});
