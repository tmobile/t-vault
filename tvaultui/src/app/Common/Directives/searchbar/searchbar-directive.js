(function () {
    'use strict';
    angular.module('vault.directives.searchbar', [])
        .directive('searchbar', Directive)
        .controller('searchbarController', Controller);

    function Directive() {
        return {
            restrict: 'E',
            link: function(scope, el, attrs, ctrl, transclude) {
            },
            scope: {
                onSearch: '='
            },
            templateUrl: 'app/Common/Directives/searchbar/searchbar.html',
            controller: 'searchbarController as vm',
            bindToController: true,
            replace: true
        }
    }

    function Controller() {
        var vm = this;
        vm.callSearch = callSearch;


        function callSearch() {
            return vm.onSearch(vm.searchValue);
        }

    }
}) ();