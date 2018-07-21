/*
* =========================================================================
* Copyright 2018 T-Mobile, US
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* See the readme.txt file for additional language around disclaimer of warranties.
* =========================================================================
*/

'use strict';

(function(app) {
    app.directive( 'tiles', function() {
        return {
            restrict: 'E',
            templateUrl: 'app/Common/Directives/tiles/tiles.html',
            scope: {
                data: '=',                             // Input data
                img: '=',                              // Name of the image to be used as tile (no need of url)
                tileFuncAvailable: '=',
                numOfTiles: '=?',
                loading: '=',
                searchValue: '=',                      // Filter string
                tileDetails : '&',     
                deleteFolder : '&',    
                editFolder : '&',                      // Function to handle click on tile
                parent : '@'
            },
            link: function( scope, element, attrs ) {
              // console.log(scope);
                scope.parent_admin = false;
                scope.imgSource = 'assets/images/' + scope.img;
                scope.tileClicked = function(e, item) {
                    if(scope.tileFuncAvailable) {
                        scope.tileDetails()(item);
                    }
                    e.stopPropagation();
                }
                //show more functionality
                if (scope.parent == 'admin'){
                    scope.parent_admin = true;
                }
			var pagesShown = 1;
		    var pageSize = 20;
		    
		    scope.paginationLimit = function(data) {
                scope.currentshown = pageSize * pagesShown;
                if(scope.currentshown >= scope.numOfTiles){
                    scope.currentshown = scope.numOfTiles;
                }
		        return scope.currentshown;
		    };
		    scope.hasMoreItemsToShow = function() {             
		        return pagesShown < (scope.numOfTiles / pageSize);
		    };
		    scope.showMoreItems = function() {
		        pagesShown = pagesShown + 1;       
		    };	

            }
        }
    } );
})(angular.module('vault.directives.tiles',[]));
