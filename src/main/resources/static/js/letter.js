$(function () {
    $("#sendBtn").click(send_letter);
    $(".close").click(delete_msg);
});

function send_letter() {
    $("#sendModal").modal("hide");//隐藏发送框
    //发送私信
    var toName = $("#recipient-name").val();//接收人名字
    var content = $("#message-text").val();//私信内容
    $.post(
        CONTEXT_PATH + "/letter/send",//请求路径
        {"toName": toName, "content": content},//请求参数
        function (data) {
            data = $.parseJSON(data);//json字符串-->json对象
            $("#hintBody").text(data.msg);//提示消息
            $("#hintModal").modal("show");
            setTimeout(function () {//显示提示框
                $("#hintModal").modal("hide");//隐藏提示框
                location.reload();//重载当前页面
            }, 2000);//两秒后执行
        }
    );
}

function delete_msg() {
    // TODO 删除数据
    $(this).parents(".media").remove();
}