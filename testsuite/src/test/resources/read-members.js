var callback = arguments[arguments.length - 1];
var path = arguments[0];

liveoak.readMembers(path, { success: function (data) {
    callback(JSON.stringify(data));
} });