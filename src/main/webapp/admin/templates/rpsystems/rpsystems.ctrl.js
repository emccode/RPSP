app.controller('rpsystemsCtrl', [ 'userService','$scope',
    function (userService,$scope) {
        userService.getAdminData().then(function(res){
            $scope.data=JSON.stringify(res, null, ' ');
        })
    }])
