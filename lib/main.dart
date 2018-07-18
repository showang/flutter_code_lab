import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_kube/CategoryPage.dart';
import 'package:flutter_kube/DiscoverPage.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;

void main() => runApp(new MyApp());

enum TabItem { discover, category }

class MyApp extends StatelessWidget {
  // This widget is the root of your application.

  final KK.KKBOXOpenAPI openApi = KK.KKBOXOpenAPI(
      "fc87971f683fd619ba46be6e3aa2cbc2", "5b70cd567551d03d4c43c5cec9e02d1a");

  @override
  Widget build(BuildContext context) {
    print("open api: $openApi");
    return new MaterialApp(
      title: 'Flutter Demo',
      theme: new ThemeData(
        primarySwatch: Colors.pink,
      ),
      home: new MyHomePage(openApi, title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage(this.api, {Key key, this.title}) : super(key: key);

  final String title;
  final KK.KKBOXOpenAPI api;

  @override
  _MyHomePageState createState() => new _MyHomePageState(api);
}

class _MyHomePageState extends State<MyHomePage> {
  final List<BottomNavigationBarItem> tabItems = [
    new BottomNavigationBarItem(
        icon: new Icon(Icons.whatshot), title: new Text("Today")),
    new BottomNavigationBarItem(
        icon: new Icon(Icons.library_music), title: new Text("Playlist"))
  ];

  int _pageIndex = 0;
  TabItem currentTab = TabItem.discover;

  KK.KKBOXOpenAPI api;

//  final _insideScaffoldKey = new GlobalKey<ScaffoldState>();

  _MyHomePageState(this.api);

  PersistentBottomSheetController bottomSheetController;

  Map<TabItem, GlobalKey<NavigatorState>> navigatorKeys = {
    TabItem.discover: GlobalKey<NavigatorState>(),
    TabItem.category: GlobalKey<NavigatorState>(),
  };

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: () async =>
          !await navigatorKeys[currentTab].currentState.maybePop(),
      child: new Scaffold(
//        body: new Scaffold(
//          key: _insideScaffoldKey,
        body: Stack(
          children: <Widget>[
            _buildOffstageNavigator(TabItem.discover),
            _buildOffstageNavigator(TabItem.category),
          ],
        ),
//        ),
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
        return CategoryPage();
    }
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
