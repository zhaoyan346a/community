$(function(){
    $("#uploadForm").submit(upload);
});

function upload() {
    $.ajax({
        url: "http://upload-z1.qiniup.com",
        method: "post",
        processData: false, //不把表单内容转成字符串
        contentType: false, //不让jquery设置上传类型
        data: new FormData($("#uploadForm")[0]),  //要传送的数据，FormData是JS中封装表单数据的对象
        success: function(data) {  //成功时的调用函数
            if(data && data.code == 0) { //七牛云返回的是json类型的数据
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) { //我自己的服务器返回的是json类型的字符串
                        data = $.parseJSON(data);//所以需要转型
                        if(data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    return false;// return false会阻止表单的自动提交。事件到此为止，不会往下继续执行
}