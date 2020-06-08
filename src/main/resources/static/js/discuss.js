//加载完页面就执行这个方法
$(function () {
    //给按钮添加点击事件
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);
});

//点赞功能
function like(btn, entityType, entityId, entityUserId, postId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType": entityType, "entityId": entityId, "entityUserId": entityUserId, "postId": postId},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {//表示操作成功（点赞或取消点赞）
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus == 1 ? "已赞" : "赞");
            } else {
                alert(data.msg);
            }
        }
    );
}

//置顶按钮单击事件
function setTop() {
    $.post(
        CONTEXT_PATH + "/discuss/top",
        {"postId": $("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $("#topBtn").attr("disabled", true);
            } else {
                alert(data.msg);
            }
        }
    );
}

//加精按钮单击事件
function setWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful",
        {"postId": $("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $("#wonderfulBtn").attr("disabled", true);
            } else {
                alert(data.msg);
            }
        }
    );
}

//删除按钮单击事件
function setDelete() {
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        {"postId": $("#postId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                window.location.href = CONTEXT_PATH + "/index";
            } else {
                alert(data.msg);
            }
        }
    );
}