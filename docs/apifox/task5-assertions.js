pm.test("HTTP status is 200", function () {
    pm.response.to.have.status(200);
});
const body = pm.response.json();
pm.test("Query parameters are echoed", function () {
    pm.expect(body.args.course).to.eql("java-framework");
    pm.expect(body.args.task).to.eql("5");
});
pm.test("Custom request header is echoed", function () {
    const key = Object.keys(body.headers).find(function (name) {
        return name.toLowerCase() === "x-lab-task";
    });
    pm.expect(key).to.be.a("string");
    pm.expect(body.headers[key]).to.eql("experiment-1-task-5");
});
