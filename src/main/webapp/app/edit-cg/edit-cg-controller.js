var app = angular.module('home');


app.controller('editCgController', ['$scope', '$http', '$modal', '$modalInstance','vmStructureService', function ($scope, $http, $modal, $modalInstance, vmStructureService) {	
	
	$scope.vmGsAndCgFlatData = {};
	$scope.protectedSelectedIndex = -1;
	
	
	$scope.initData = function(){
		$scope.vmGsAndCgFlatData = vmStructureService.getCachedVmGsAndCgFlatData();
		$scope.protectedSelectedIndex = vmStructureService.getProtectedSelectedIndex();
		$scope.cgVms = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].vms;
		
		$scope.vmStructureData = vmStructureService.getCachedVmStructureData();
		$scope.unprotectedVms = $scope.vmStructureData.unprotectedVms;
		
		$scope.cgName = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].name;
		
		$scope.cgVmsJoinedCandidates = new Array();
		$scope.selectedVms = new Array();
		
		for(i = 0; i < $scope.cgVms.length; i++){
			var currVm = $scope.cgVms[i];
			var currVmCloned = JSON.parse(JSON.stringify(currVm));
			$scope.selectedVms.push(currVmCloned);
			$scope.cgVmsJoinedCandidates.push(currVmCloned);
		}
		
		for(i = 0; i < $scope.unprotectedVms.length; i++){
			var currVm = $scope.unprotectedVms[i];
			var currVmCloned = JSON.parse(JSON.stringify(currVm));
			$scope.cgVmsJoinedCandidates.push(currVmCloned);
		}
		
		$scope.selectedPackage = {};
		for(i = 0; i < $scope.vmStructureData.systemInfo.packages.length; i++){
			var currPackage = $scope.vmStructureData.systemInfo.packages[i];
			if(currPackage.id == $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].packageId){
				$scope.selectedPackage = currPackage;
			}
		}
		
		$scope.enableReplication = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].enableProtection;
		$scope.priceSlider = 150;
		$scope.selectedCopy = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].replicaClusters[0].groupCopySettings[0];
		

	};
	   
	$scope.initData();
	
	
	$scope.editCg = function(){
		var currCg = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex];
		
		var currCgCloned = JSON.parse(JSON.stringify(currCg));
		currCgCloned.replicaClusters[0].groupCopySettings[0].snapshots = [];
    	
    	var currCgModified = JSON.parse(JSON.stringify(currCgCloned));
    	currCgModified.name = $scope.cgName;
    	currCgModified.packageId = $scope.selectedPackage.id;
    	currCgModified.packageName = $scope.selectedPackage.name;
    	currCgModified.packageDisplayName = $scope.selectedPackage.displayName;
    	currCgModified.vms = $scope.selectedVms;
    	currCgModified.enableProtection = $scope.enableReplication;
    	
    	var cgId = currCg.id;
    	var cgChanges = {};
    	cgChanges.originalConsistencyGroup = currCgCloned;
    	cgChanges.currentConsistencyGroup = currCgModified;
    		
    	vmStructureService.editCg(cgId, cgChanges);  
    	/*$modalInstance.dismiss('cancel');*/
	}
	
	
	$scope.cancel = function(){
		$modalInstance.dismiss('cancel');
	}
	   
    
}]);