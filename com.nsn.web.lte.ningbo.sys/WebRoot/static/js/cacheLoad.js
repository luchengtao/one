N.CacheLoad = N.Class.extend({
    initialize:function(){
        var me = this;
        //N.Util.cities('city');
        $('#cache_query').on('click',function(){
            N.Util.reloadData('page_table');
        });
        $('#cache_add').on('click',function(){
            N.Util.clearForm('cacheForm');
            $('#cache_modal').modal();
        });

        $('#cache_save').on('click',function(){
            layer.load(1);
            $.ajax({
                type: "POST",
                url:'/sys/cacheLoad/saveCache',
                data:$('#cacheForm').serialize(),// 你的formid
                dataType:'json',
                success: function(data) {
                    layer.closeAll('loading');
                    layer.msg(data.msg);
                    N.Util.reloadData('page_table');
                    $('#cache_modal').modal('hide');
                }
            });
        });
        $('#cache_load').on('click',function(){
        	layer.confirm('是否重新加载下方配置的缓存（这不仅是清除缓存）？', {icon: 3, title:'提示'}, function(){
        		$.ajax({
                    type: "POST",
                    url:'/sys/cacheLoad/loadCache'
                });
        	});
        	
        });
        $('#cache_clean').on('click',function(){
        	$.ajax({
                type: "POST",
                url:'/sys/cacheLoad/cleanAllCache',
                dataType:'json',
                success: function(data) {
                	if(data.isOk){
                		layer.msg("清除成功");
                	}else{
                		layer.msg("清除失败");
                	}
                }
            });
        });
    },
    
    dtObj:{},
    loadData:function(){
    	this.dtObj = $('#page_table').DataTable({
            "destroy": true,
            "processing": true,
            "serverSide": true,
            "bFilter": false, // 过滤功能
            "iDisplayLength":20,
            "order":[[2,"asc"],[3,"asc"]],
            "bLengthChange": false, // 改变每页显示数据数量
            "ajax":  {
                "url": "/sys/cacheLoad/getPage",
                "data": function(d){
                    return $.extend({}, d, N.Util.serializeJson('searchForm'));
                }
            },
            "columns": [
            	{
					"class":          "details-control",
					"orderable":      false,
					"data":           null,
					"defaultContent": ""
		    	},
                {"data": "module"},
                {"data": "name"},
                {"data": "uri"},
                {"data": "params", "visible": false},
                {"data": "remark"},
                {render : function(data, type, row) {
                    var editBtn = '<a href="javascript:cacheLoad.edit(\''+row.id+'\');" class="btn btn-primary btn-xs btn-edit">编辑</a>&nbsp;&nbsp;';
                    var delBtn = '<a href="javascript:cacheLoad.delete(\''+row.id+'\');" class="btn btn-danger btn-xs btn-del">删除</a>';
                    return editBtn + delBtn;
                  }
                }
            ]
        });
    },
    edit:function(id){
        var me = this;
        layer.load(1);
        $.ajax({
            type: "POST",
            url:"/sys/cacheLoad/getCache",
            data:{"id":id},// id
            dataType:'json',
            success: function(data) {
                layer.closeAll('loading');
                N.Util.loadForm('cacheForm',data);
                $('#cache_modal').modal();
            }
        });
    },
    delete:function(cid){
        layer.confirm('是否删除？', {icon: 3, title:'提示'}, function(){
            layer.load(1);
            $.ajax({
                type: "POST",
                url:"/sys/cacheLoad/delCache",
                data:{"id":cid},// id
                dataType:'json',
                error: function(request) {
                    layer.closeAll('loading');
                    layer.msg('操作失败');
                },
                success: function(data) {
                    layer.closeAll('loading');
                    if(!data.isOk){
                        layer.msg('操作失败');
                    }else{
                        layer.msg('删除成功');
                        N.Util.reloadData('page_table');
                    }
                }
            });
        });
    },
    
    params_detail: function( d ) {
		return '<b>&nbsp;&nbsp;&nbsp;&nbsp;参数:&nbsp;&nbsp;</b>&nbsp;&nbsp;'+d.params+'<br>';
	},
});
var cacheLoad = new N.CacheLoad();
$(function(){
    cacheLoad.loadData();
    $('#page_table tbody').on( 'click', 'tr td.details-control', function () {
    	var tr = $(this).closest('tr');
    	var row = cacheLoad.dtObj.row( tr );
    	
    	if ( row.child.isShown() ) {
    		tr.removeClass( 'details' );
    		row.child.hide();
    	}
    	else {
    		tr.addClass( 'details' );
    		row.child( cacheLoad.params_detail( row.data() ) ).show();
    	}
    });
});