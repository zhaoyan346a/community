$(function () {
    $(".follow-btn").click(follow);
});

function follow() {
    var btn = this;
    var url;
    if ($(btn).hasClass("btn-info")) {//按钮处于“关注他” 状态
        // 关注TA
        url = CONTEXT_PATH + "/follow";
        //$(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
    } else {//按钮处于“已关注” 状态
        // 取消关注
        url = CONTEXT_PATH + "/unfollow";
        //$(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
    }
    $.post(
        url,
        //当前元素的前一个元素的val;entityType=3表示USER
        {"entityType": 3, "entityId": $(btn).prev().val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                //这个地方是ajax请求后改变样式的，前端的东西先不去做了，直接reload()吧。
                window.location.reload();
            } else {
                alert(data.msg);
            }
        }
    );
}