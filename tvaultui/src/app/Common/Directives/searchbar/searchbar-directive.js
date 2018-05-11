(function () {
    'use strict';
    angular.module('vault.directives.searchbar', [])
        .directive('searchbar', Directive)
        .controller('searchbarController', Controller);

    function Directive() {
        return {
            restrict: 'E',
            link: function(scope, el, attrs, ctrl, transclude) {},
            templateUrl: 'app/Common/Directives/searchbar/searchbar.html',
            controller: 'searchbarController as vm',
            bindToController: true,
            replace: true
        }
    }

    function Controller($rootScope, $scope) {
        var vm = this;
        vm.callSearch = callSearch;

        $scope.$watch(function () {
          return vm.searchValue;
        }, callSearch);


        function callSearch() {
            return $rootScope.$broadcast('search', vm.searchValue);
        }

    }
}) ();