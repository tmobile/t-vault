'use strict';

var path = require( 'path' );
var gulp = require( 'gulp' );
var conf = require( './conf' );
var shell = require( 'gulp-shell' );


//replace with your endpoint
var proxyLiveTarget = 'http://localhost:8080';

//Assumption : wiremock is pointing to port 9005
var proxyMockTarget = 'http://localhost:9006';
var context = '/vault/';
var browserSync = require( 'browser-sync' );
var browserSyncSpa = require( 'browser-sync-spa' );

var util = require( 'util' );

var proxyMiddleware = require( 'http-proxy-middleware' );

function browserSyncInit( baseDir, browser, env ) {
    browser = browser === undefined ? 'default' : browser;

    var routes = null;

    var options = {
        target: '',
        changeOrigin: '',
        https: '',
        secure: '',
        host: '0.0.0.0'
    };

    if ( baseDir === conf.paths.src || ( util.isArray( baseDir ) && baseDir.indexOf( conf.paths.src ) !== -1 ) ) {
        routes = {
            '/bower_components': 'bower_components'
        };
    }

    var server = {
        baseDir: baseDir,
        routes: routes
    };

    if ( env === 'mock' ) {
        conf.constants.env = "mock";
        options.target = proxyMockTarget;
        options.changeOrigin = false;
        options.https = false;
        options.secure = true;
    }

    if ( env ) {
        if ( env === 'mock' ) {
          conf.constants.env = "mock";
            options.target = proxyMockTarget;
            options.changeOrigin = false;
            options.https = false;
            options.secure = true;
        } else if ( env === 'live' ) {
            options.target = proxyLiveTarget;
            options.changeOrigin = true;
            options.https = false;
            options.secure = false;
        }

        server.middleware = proxyMiddleware( context, options );
    }

    browserSync.instance = browserSync.init( {
        startPath: '/',
        server: server,
        browser: browser
    } );
}

browserSync.use( browserSyncSpa( {
    selector: '[ng-app]' // Only needed for angular apps
} ) );

gulp.task( 'serve:mock', [ 'watch' ], function() {
    browserSyncInit( [ path.join( conf.paths.tmp, '/serve' ), conf.paths.src ], null, 'mock' );
} );

gulp.task( 'serve', [ 'clean', 'watch' ], function() {
    browserSyncInit( [ path.join( conf.paths.tmp, '/serve' ), conf.paths.src ] );
} );

gulp.task( 'serve:live', [ 'clean', 'watch' ], function() {
    browserSyncInit( [ path.join( conf.paths.tmp, '/serve' ), conf.paths.src ], null, 'live' );
} );

gulp.task( 'serve:mock-backend', [ 'clean' ], function() {
    conf.constants.env = "mock";
    gulp.start( 'wire-mock' );
    gulp.start( 'serve:mock' );
} );

gulp.task( 'serve:dist', [ 'build' ], function() {
    browserSyncInit( [conf.paths.dist], null, 'live' );
} );

gulp.task( 'serve:e2e', [ 'inject' ], function() {
    browserSyncInit( [ conf.paths.tmp + '/serve', conf.paths.src ], [] );
} );

gulp.task( 'serve:e2e-dist', [ 'build' ], function() {
    browserSyncInit( conf.paths.dist, [] );
} );
