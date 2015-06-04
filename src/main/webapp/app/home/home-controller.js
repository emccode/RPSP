angular.module('home',  ['pascalprecht.translate', 'locale'])
  .controller('homeController', ['$scope', '$http', function($scope, $http) {   
	  $scope.currentUser = {};
	  $scope.welcomeData = {};
	  $scope.getCurrentUser = function(){
		    $http.get('users/current-user')
		    .success(function(data,status,headers,config){
		        $scope.currentUser = data;
		        var account;
		        var user;
		        
		        if(data.account.label != null){
		        	account = data.account.label;
		        }
		        else{
		        	account = data.account.name;
		        }

		        user = data.firstName + ' ' + data.lastName; 
		        $scope.welcomeData = user + '@' + account;
		    })		   
	   };
	   $scope.getCurrentUser();
}])


angular.module('home').controller('vmStructureController', ['$scope', '$http', function ($scope, $http) {
	
	$scope.vmStructureData = {};
	$scope.vmGsAndCgFlatData = {};
	$scope.totalVms = {};
	$scope.protectedVms = {};
	
	$scope.getVmStructureData = function(){
		    $http.get('/rpsp/account-vms')
		    .success(function(data,status,headers,config){
		        $scope.vmStructureData = data;
		        
		        //flatten the hierarchical data to be displayed in table
		        var vmGsAndCgFlatDataArr = new Array();
		        var topLevelContainers = $scope.vmStructureData.protectedVms;
		        var length = topLevelContainers.length;
		        for (var i = 0; i < length; i++) {
		            var currVmContainer = topLevelContainers[i];
		            vmGsAndCgFlatDataArr.push(currVmContainer);
		            if(currVmContainer.consistencyGroups != null){
		            	for(var j = 0; j < currVmContainer.consistencyGroups.length; j++){
		            		var currNestedCG = currVmContainer.consistencyGroups[j];
		            		currNestedCG.parent = currVmContainer.name;
		            		vmGsAndCgFlatDataArr.push(currNestedCG);
		            	}
		            }
		        }
		        $scope.vmGsAndCgFlatData = vmGsAndCgFlatDataArr;
		        
		        //count protected vms
		        var protectedVmsCount = 0;
		        length = vmGsAndCgFlatDataArr.length;
		        for (var i = 0; i < length; i++) {
		            var currVmContainer = vmGsAndCgFlatDataArr[i];
		            if(currVmContainer.vms != null){
		            	protectedVmsCount += currVmContainer.vms.length;
		            }
		        }
		        
		        //count unprotected vms
		        var unprotectedVmsCount = 0;
		        if($scope.vmStructureData.unprotectedVms != null){
		        	unprotectedVmsCount = $scope.vmStructureData.unprotectedVms.length;
		        }
		        else{
		        	$scope.vmStructureData.unprotectedVms = new Array();
		        	unprotectedVmsCount = 0;
		        }
		        
		        //summary
		        $scope.totalVms = protectedVmsCount + unprotectedVmsCount;
		        $scope.protectedVms = protectedVmsCount;
		        
		        
		        
		    })		   
	};
	   
	$scope.getVmStructureData();
	
    
    $scope.protectedSelectedIndex = -1;
    $scope.unprotectedSelectedIndex = -1;
    $scope.toggleSelect = function(ind, isProtected){
    	if(isProtected == true){
	        if( ind === $scope.protectedSelectedIndex ){
	            $scope.protectedSelectedIndex = -1;
	        } else{
	            $scope.protectedSelectedIndex = ind;
	        }
	        $scope.unprotectedSelectedIndex = -1;
    	}
    	else{
	        if( ind === $scope.unprotectedSelectedIndex ){
	            $scope.unprotectedSelectedIndex = -1;
	        } else{
	            $scope.unprotectedSelectedIndex = ind;
	        }
	        $scope.protectedSelectedIndex = -1;
    	}
    }
    
    
    $scope.moveVm = function(vmId, sgId) {

    	//this is protect
    	if(sgId !== undefined){
	    	var unprotectedVms = $scope.vmStructureData.unprotectedVms;
	        for (var i = 0; i < unprotectedVms.length; i++) {
	 
	            var currVm = unprotectedVms[i];
	                 
	            if (currVm.id == vmId) {
	            	var allCgAndGs = $scope.vmGsAndCgFlatData;
	            	for (var j = 0; j < allCgAndGs.length; j++) {
	            		if(allCgAndGs[j].id == sgId){
	            			allCgAndGs[j].vms.push(currVm);
	            		}
	            	}
	 
	                unprotectedVms.splice(i, 1);
	            }
	        }
    	}
    	//this is unprotect
    	else{
    		var allCgAndGs = $scope.vmGsAndCgFlatData
    		for (var i = 0; i < allCgAndGs.length; i++) {
    			
    			//this is not group set
    			if(allCgAndGs[i].type == 'cg'){
	    			for (var j = 0; j < allCgAndGs[i].vms.length; j++) {
	    				
	    				var currVm = allCgAndGs[i].vms[j];
	    				
	            		if(currVm.id == vmId){
	            			$scope.vmStructureData.unprotectedVms.push(currVm);
	            			allCgAndGs[i].vms.splice(j, 1);
	            		}
	            	}
    			}
        	}
    	}
 
        $scope.$apply();
    };
    
    
    $scope.imageAccess = function(enable){
    	var currCg = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex];
    	var cgId = currCg.id;
    	var replicaClusterId = currCg.replicaClusters[0].id;
    	var copyId = currCg.replicaClusters[0].groupCopySettings[0].id;
    	var url;
    	if(enable == true){
    	   url = '/rpsp/image-access/enable' + '?' + 'clusterId=' + replicaClusterId + '&' + 'groupId=' + cgId + '&' + 'copyId=' + copyId;
    	}
    	else{
    	   url = '/rpsp/image-access/disable' + '?' + 'clusterId=' + replicaClusterId + '&' + 'groupId=' + cgId + '&' + 'copyId=' + copyId;
    	}
    	   	
	    $http.put(url)
	    .success(function(data,status,headers,config){
	        
	    })	    	
    	
    }
    
    
    
}]);


angular.module('home').directive('draggable', function() {
	return {
        restrict: "A",
        link: function(scope, element, attributes, ctlr) {
            element.attr("draggable", true);
 
            element.bind("dragstart", function(eventObject) {
                eventObject.dataTransfer.setData("text", attributes.vmid);
            });
        }
    };
});


angular.module('home').directive('droppable', function() {
    return {
        restrict: "A",
        link: function (scope, element, attributes, ctlr) {
 
            element.bind("dragover", function(eventObject){
                eventObject.preventDefault();
            });
 
            element.bind("drop", function(eventObject) {
                 
                // invoke controller/scope move method
                scope.moveVm(eventObject.dataTransfer.getData("text"), attributes.cgid);
 
                // cancel actual UI element from dropping, since the angular will recreate a the UI element
                eventObject.preventDefault();
            });
        }
    };
});


angular.module('home').run(['localeService', function(localeService){
	localeService.setLocale();
}]);


angular.module('home').config(function ($translateProvider) {

  $translateProvider.useStaticFilesLoader({
    prefix: 'locales/locale-',
    suffix: '.json'
  });
  
});