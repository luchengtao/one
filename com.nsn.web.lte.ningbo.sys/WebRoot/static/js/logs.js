N.Logs = N.Class.extend({
	loadData:function(){
		
		$('#page_table').DataTable({
			"destroy": true,
		    "processing": true,
		    "serverSide": true,
		    "bFilter": false, // 过滤功能
		    "iDisplayLength":20,
		    "bLengthChange": false, // 改变每页显示数据数量
		    "order":[[6,"desc"]],
		    "ajax":  {
		        "url": "/sys/logs/getPage",
		        "data": function(d){
		        	return $.extend({}, d, N.Util.serializeJson('searchForm'));
		        }
		    },
		    "columns": [
		        {"data": "user_id"},
		        {"data": "user_name"},
		        {"data": "ip"},
		        {"data": "module"},
		        {	"data": "params",
		        	"orderable":false,
		        	"render": function(data, type, row) {
		        		data = data.toString();
		        		if (data.length>80) { 
		        			var html ="<div style='width:300px;overflow:hidden;text-overflow:ellipsis;display:inline-flex;'>"+data+"</div>";
		        			html+="<a onclick='logs.showparams(this)' data-flag='1' class='pull-right'>[展开]</a>"
		        			return html; 
		        		} 
		        		else { return data; }
		        	}
		        },
		        {"data": "remark"},
		        {"data": "create_time"}
		    ]
		});
	},
	
	showparams: function(obj,flag){
		if($(obj).data("flag")=="1"){
			$(obj).prev().css("overflow","visible");
			$(obj).prev().css("word-break", "break-all");
			$(obj).prev().css("white-space", "initial");
			$(obj).html("[收起]");
			$(obj).data("flag","0");
		}else{
			$(obj).prev().css("overflow","hidden");
			$(obj).prev().css("word-break", "initial");
			$(obj).prev().css("white-space", "inherit");
			$(obj).html("[展开]");
			$(obj).data("flag","1");
		}
	}
});

var logs = new N.Logs();

$(function(){
	logs.loadData();
});
