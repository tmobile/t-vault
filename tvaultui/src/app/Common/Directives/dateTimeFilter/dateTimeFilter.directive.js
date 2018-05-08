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
    app.directive( 'dateTimeFilter', function( $rootScope, $window, $filter ) {
        return {
            restrict: 'E',
            templateUrl: 'app/Common/Directives/dateTimeFilter/dateTimeFilter.html',
            scope: {
                parentObj: '='
            },
            link: function( scope ) {
                scope.modelData = {};
                scope.modelData.displayLabel = 'LAST 6 MONTHS';
                scope.parentObj.timeFilterRadioModel = scope.parentObj.timeFilterRadioModel || 'timePeriodVal';
                /*scope.selectBoxChanged = function() {
                    //alert( scope.parentObj.singleSelect );
                    scope.isSelectionMade = true;
                    //scope.selectedLabelValue = true;
                    scope.selectedLabelValue = scope.createDateFilterLabel();
                    console.log( scope.selectedLabelValue );
                    console.log( scope.isSelectionMade );
                };*/
                /*scope.fromDateChanged = function() {
                    scope.modelData.isFromDatePickerActive = false;
                };

                scope.toDateChanged = function() {
                    scope.modelData.isToDatePickerActive = false;
                };*/

                scope.triggerTimeFiltering = function() {
                    if ( scope.isSelectionMade ) {
                        // scope.parentObj.isTimeFilterActive = false;
                        //scope.modelData.selectedLabelText = scope.modelData.tempLabelText;
                        var labelValue = scope.createDateFilterLabel();
                        if ( labelValue ) {
                            scope.parentObj.isTimeFilterActive = false;
                            scope.modelData.selectedLabelText = labelValue;
                            $rootScope.$broadcast( 'reRenderAllTiles' );
                            scope.isSelectionMade = false;
                            scope.selectedLabelValue = false;
                        }
                    }
                };

                scope.dateSelectBoxChanged = function() {
                    console.log( scope.modelData.selectedItem );
                    scope.isSelectionMade = true;

                    //scope.isSelectionMade = true;
                    //scope.selectedLabelValue = true;
                    scope.selectedLabelValue = true;
                    console.log( scope.selectedLabelValue );
                    console.log( scope.isSelectionMade );

                    //scope.modelData.tempLabelText = scope.modelData.selectedItem.name;
                };

                scope.dateFilterRadioChange = function(val) {
                  
                    scope.isSelectionMade = false;
                    scope.selectedLabelValue = false;
                    scope.parentObj.timeFilterRadioModel = val;
                };
                scope.callModel = function(val){

                  scope.parentObj.timeFilterRadioModel = val;
                }

                scope.checkProceedStatus = function() {
                    if ( scope.modelData.createdDate && scope.modelData.endDate ) {
                        scope.selectedLabelValue = true;
                    } else {
                        scope.selectedLabelValue = false;
                    }
                };

                scope.fromDateInputChanged = function( value ) {
                    scope.isSelectionMade = true;
                    scope.checkProceedStatus();
                    //scope.selectedLabelValue = scope.createDateFilterLabel();
                    //scope.modelData.createdDate = $window.moment( value ).format( 'X' ); // to assign the updated from date
                };

                scope.toDateInputChanged = function( value ) {
                    scope.isSelectionMade = true;
                    scope.checkProceedStatus();
                    //scope.selectedLabelValue = scope.createDateFilterLabel();
                    //scope.modelData.endDate = $window.moment( value ).format( 'X' ); // to assign the updated to date
                };

                scope.createDateFilterLabel = function() {
                    var returnData;
                    if ( scope.parentObj.timeFilterRadioModel === 'timePeriodVal' ) {
                        returnData = scope.modelData.selectedItem.name;
                    } else if ( scope.parentObj.timeFilterRadioModel === 'dateRangeVal' ) {
                        //scope.modelData.selectedLabelText = scope.modelData.createdDate + ' - ' + scope.modelData.endDate;
                        if ( scope.modelData.createdDate && scope.modelData.endDate ) {
                            //returnData = 'DATE RANGE'; // This needs to be changed in future. Should as per the selected dates
                            //returnData = scope.modelData.createdDate + ' - ' + scope.modelData.endDate;
                            //console.log( $( '.from-date-input' ).val() );
                            returnData =
                                $filter( 'date' )( new Date( $( '.from-date-input' ).val() ), 'M/d/yy' ) + ' - ' + $filter( 'date' )( new Date( $( '.to-date-input' ).val() ), 'M/d/yy' );
                            //returnData = $( '.from-date-input' ).val() + ' - ' + $( '.to-date-input' ).val();
                        } else {
                            returnData = null;
                        }
                    }
                    return returnData;
                };

                scope.itemArray = [ {
                        id: 1,
                        name: 'Last 6 Months'
                    }, {
                        id: 2,
                        name: 'Last 9 Months'
                    }, {
                        id: 3,
                        name: 'Last 12 Months'
                    }

                ];
                scope.modelData.selectedItem = scope.itemArray[ 0 ];
                scope.modelData.selectedLabelText = scope.modelData.selectedItem.name;
            }
        }
    } );

    app.directive( "dategetter",
        function( $window ) {

            function link( $scope, element, attrs, ctrl ) {

                // Init JQuery datepicker
                element.datepicker( {
                    autoclose: true,
                    clearBtn: true,
                } );

                ctrl.$parsers.push( function( valueFromInput ) {
                    // Format displayed value in timestamp format and store it to ngModel
                    return $window.moment( valueFromInput ).format( 'X' );
                } );

            }
            /* ********** This part of code works **********  */
            return {
                restrict: 'A',
                require: 'ngModel',
                link: link
            };
        }
    );
})(angular.module('vault.directives.dateTimeFilter',[]));
