import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_kube/CategoryPage.dart';
import 'package:flutter_kube/DiscoverPage.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:kube_player_plugin/kube_player_plugin.dart';

void main() => runApp(new MyApp());

enum TabItem { discover, category }

class MyApp extends StatelessWidget {
  // This widget is the root of your application.

  final KK.KKBOXOpenAPI openApi = KK.KKBOXOpenAPI(
      "fc87971f683fd619ba46be6e3aa2cbc2", "5b70cd567551d03d4c43c5cec9e02d1a");

  @override
  Widget build(BuildContext context) {
    print("open api: $openApi");
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        backgroundColor: Colors.white,
        primarySwatch: Colors.pink,
      ),
      home: MyHomePage(openApi, title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage(this.api, {Key key, this.title}) : super(key: key);

  final String title;
  final KK.KKBOXOpenAPI api;

  @override
  MyHomePageState createState() => MyHomePageState(api);
}

class MyHomePageState extends State<MyHomePage> {
  final List<BottomNavigationBarItem> tabItems = [
    new BottomNavigationBarItem(
        icon: new Icon(Icons.whatshot), title: new Text("Today")),
    new BottomNavigationBarItem(
        icon: new Icon(Icons.library_music), title: new Text("歌單"))
  ];

  final nowPlayingHeight = 52.0;

  int _pageIndex = 0;
  TabItem currentTab = TabItem.discover;

  KK.KKBOXOpenAPI api;

//  final _insideScaffoldKey = new GlobalKey<ScaffoldState>();

  MyHomePageState(this.api);

  PersistentBottomSheetController bottomSheetController;

  static Map<TabItem, GlobalKey<NavigatorState>> navigatorKeys = {
    TabItem.discover: GlobalKey<NavigatorState>(),
    TabItem.category: GlobalKey<NavigatorState>(),
  };

  Map<String, dynamic> trackInfoMap;
  bool isPlaying = false;

  @override
  void initState() {
    super.initState();
    KubePlayerPlugin.listenEvents(startPlay: (trackInfoMap) {
      print('event startPlay:$trackInfoMap');
      setState(() {
        this.trackInfoMap = trackInfoMap;
        this.isPlaying = true;
      });
    }, stateChanged: (trackInfoMap, isPlaying) {
      print('event stateChanged:$trackInfoMap');
      setState(() {
        this.trackInfoMap = trackInfoMap;
        this.isPlaying = isPlaying;
      });
    }, stopPlay: () {
      print('event stopPlay');
      setState(() {
        this.trackInfoMap = null;
        this.isPlaying = false;
      });
    });
    KubePlayerPlugin.currentTrack();
  }

  @override
  Widget build(BuildContext context) {
    var columnChildren = <Widget>[
      Expanded(
        child: Stack(
          children: <Widget>[
            _buildOffstageNavigator(TabItem.discover),
            _buildOffstageNavigator(TabItem.category),
          ],
        ),
      ),
      trackInfoMap == null
          ? null
          : buildNowPlayingBar(trackInfoMap['coverUrl'], trackInfoMap['name'],
              trackInfoMap['artistName'], isPlaying)
    ];
    columnChildren.removeWhere((w) => w == null);
    return WillPopScope(
      onWillPop: () async =>
          !await navigatorKeys[currentTab].currentState.maybePop(),
      child: new Scaffold(
        body: Column(
          children: columnChildren,
        ),
        bottomNavigationBar: new BottomNavigationBar(
          currentIndex: _pageIndex,
          items: tabItems,
          onTap: (index) {
            setState(() {
              if (_pageIndex == index) {
                var currentSubPageState =
                    navigatorKeys[currentTab].currentState;
                currentSubPageState.popUntil((route) {
                  return route.isFirst;
                });
              } else {
                _pageIndex = index;
                switch (index) {
                  case 0:
                    currentTab = TabItem.discover;
                    break;
                  case 1:
                    currentTab = TabItem.category;
                    break;
                }
              }
            });
          },
        ),
      ),
    );
  }

  Widget _buildOffstageNavigator(TabItem tabItem) {
    return new Offstage(
      offstage: tabItem != currentTab,
      child: KubeNavigator(
        api: api,
        initPage: _buildPageWidget(tabItem),
        navigatorKey: navigatorKeys[tabItem],
      ),
    );
  }

  Widget _buildPageWidget(TabItem tabItem) {
    switch (tabItem) {
      case TabItem.discover:
        return DiscoverPage(api);
      case TabItem.category:
      default:
        return CategoryPage(api);
    }
  }

  Widget buildNowPlayingBar(
      String coverUrl, String trackName, String artistName, bool isPlaying) {
    return GestureDetector(
      onTap: () {
        KubePlayerPlugin.openNowPlaying();
      },
      child: Container(
        color: Colors.black54,
        height: nowPlayingHeight,
        child: Row(
          children: <Widget>[
            Expanded(
              child: Row(
                children: <Widget>[
                  Image.network(
                    coverUrl,
                    height: nowPlayingHeight,
                    width: nowPlayingHeight,
                  ),
                  Expanded(
                    child: Container(
                      margin: const EdgeInsets.only(left: 16.0, right: 16.0),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            trackName,
                            textAlign: TextAlign.left,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(color: Colors.white),
                          ),
                          Text(
                            artistName,
                            textAlign: TextAlign.left,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(color: Colors.white),
                          )
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            GestureDetector(
              child: Container(
                height: nowPlayingHeight,
                width: nowPlayingHeight,
                child: Icon(
                  isPlaying ? Icons.pause : Icons.play_arrow,
                  color: Colors.white,
                ),
              ),
              onTap: () {
                isPlaying
                    ? KubePlayerPlugin.pause()
                    : KubePlayerPlugin.resumePlay();
              },
            ),
          ],
        ),
      ),
    );
  }
}

class KubeNavigator extends StatelessWidget {
  KubeNavigator(
      {@required this.api, @required this.initPage, this.navigatorKey});

  final KK.KKBOXOpenAPI api;
  final GlobalKey<NavigatorState> navigatorKey;
  final Widget initPage;

  WidgetBuilder routePageBuilder(String routName,
      {KK.PlaylistInfo playlistInfo, String heroTag}) {
    switch (routName) {
      case "/":
      default:
        return (context) => initPage;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Navigator(
      key: navigatorKey,
      onGenerateRoute: (routeName) {
        return CupertinoPageRoute(
            // Default route???
            builder: (context) {
          return routePageBuilder(routeName.name)(context);
        });
      },
    );
  }
}
