N.GisCell = N.Class.extend({
    initialize:function(){
        var me = this;
        me.nsnMap = L.nsnMap('map', N.GIS_CFG);
        me.map = me.nsnMap.getMap();
        me.map.on('click', function(e) {
            if (e.latlng) {
                me.addMarker(e.latlng);
                $('#latitude').val(e.latlng.lat);
                $('#longitude').val(e.latlng.lng);
                $('#prj_type').val('GCJ02');
            }
        });

        $('#mapTab').on('shown.bs.tab', function() {
            setTimeout(function() {
                var lat = $('#latitude').val();
                var lng = $('#longitude').val();
                if (lat && lng) {
                    gisCell.addMarker([lat, lng]);
                    gisCell.map.setView([lat, lng], 16);
                }
                gisCell.map.invalidateSize();
            }, 100);
        });
        //N.Util.cities('city');
        $('#cell_query').on('click',function(){
            N.Util.reloadData('page_table');
        });
        $('#cell_add').on('click',function(){
            N.Util.clearForm('cellForm');
            $("#isNew").val(true);
            $('#cell_modal').modal();
        });
        $('#cell_import').on('click',function(){
            $('#import_save').removeAttr('disabled');
            cellFile.files[0] = null;
            $('#cellFile').val("");
            $('#import_modal').modal();
        });
        
        $('#cell_clear').on('click',function(){
        	layer.confirm('确定清空数据？', {icon: 3, title:'提示'}, function(){
                layer.load(1);
                $.ajax({
                    type: "POST",
                    url:"/sys/gisCell/clearCell",
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
                            layer.msg('清空成功');
                            N.Util.reloadData('page_table');
                        }
                    }
                });
            });
        });
        
        $('#import_save').on('click', function() {
            $(this).attr('disabled', 'true');
            var file = cellFile.files[0];
            if (!file) {
                layer.msg('请选择要上传的文件！');
                return;
            }
            var dForm = new FormData();
            dForm.append("cellFile", file, file.name);
            dForm.append("rate", $('#rate').val());
            
            layer.load(1);
            $('#import_save').removeAttr('disabled');
            $('#import_modal').modal('hide');
            $.ajax({
                url: '/sys/gisCell/impCell',
                data: dForm,
                dataType: 'text',
                processData: false,
                contentType: false,
                type: 'POST',
                success: function(json) {
                    layer.closeAll('loading');
                    N.Util.reloadData('page_table');
                }
            });
        });

        $('#cell_save').on('click',function(){
            layer.load(1);
            $.ajax({
                type: "POST",
                url:'/sys/gisCell/saveCell',
                data:$('#cellForm').serialize(),// 你的formid
                dataType:'json',
                success: function(data) {
                    layer.closeAll('loading');
                    layer.msg(data.msg);
                    $('#cell_modal').modal('hide');
                    N.Util.reloadData('page_table');
                }
            });
        });
    },
    loadData:function(){
        $('#page_table').DataTable({
            "destroy": true,
            "processing": true,
            "serverSide": true,
            "bFilter": false, // 过滤功能
            "iDisplayLength":20,
            "bLengthChange": false, // 改变每页显示数据数量
            "ajax":  {
                "url": "/sys/gisCell/getPage",
                "data": function(d){
                    return $.extend({}, d, N.Util.serializeJson('searchForm'));
                }
            },
            "columns": [
                {"data": "city"},
                {"data": "area"},
                {"data": "site_name"},
                {"data": "cell_id"},
                {"data": "cell_name"},
                {"data": "cover"},
                {"data": "site_type"},
                {"data": "site_band"},
                {"data": "longitude"},
                {"data": "latitude"},
                {"data": "azimuth"},
                {"data": "radius"},
                {"data": "beamwidth"},
                {"data": "network"},
                {render : function(data, type, row) {
                    var editBtn = '<a href="javascript:gisCell.edit('+row.cell_id+');" class="btn btn-primary btn-xs btn-edit">编辑</a>&nbsp;&nbsp;';
                    var delBtn = '<a href="javascript:gisCell.delete('+row.cell_id+');" class="btn btn-danger btn-xs btn-del">删除</a>'
                    return editBtn + delBtn;
                  }
                }
            ]
        });
    },
    importData:function(){
       $('#import_modal').modal();
    },
    edit:function(cid){
        var me = this;
        layer.load(1);
        $.ajax({
            type: "POST",
            url:"/sys/gisCell/getCell",
            data:{"id":cid},// id
            dataType:'json',
            success: function(data) {
                layer.closeAll('loading');
                N.Util.loadForm('cellForm',data);
                $("#isNew").val(false);
                $('#cell_modal').modal();
                N.Util.reloadData('page_table');
            }
        });
    },
    delete:function(cid){
        layer.confirm('是否删除？', {icon: 3, title:'提示'}, function(){
            layer.load(1);
            $.ajax({
                type: "POST",
                url:"/sys/gisCell/delCell",
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
    addMarker: function(latlng) {
        if (!this.marker) {
            this.marker = L.circleMarker(latlng).addTo(this.map);
        } else {
            this.marker.setLatLng(latlng);
        }
    }

});
var gisCell = new N.GisCell();
$(function(){
    gisCell.loadData();
});