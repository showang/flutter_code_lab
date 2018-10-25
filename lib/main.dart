import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_kube/CategoryPage.dart';
import 'package:flutter_kube/DiscoverPage.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:kube_player_plugin/kube_player_plugin.dart';
import 'package:multi_navigator_bottom_bar/multi_navigator_bottom_bar.dart';

void main() => runApp(MyApp());

enum TabItem { discover, category }

class MyApp extends StatelessWidget {
  // This widget is the root of your application.

  static final KK.KKBOXOpenAPI openApi = KK.KKBOXOpenAPI(
      "fc87971f683fd619ba46be6e3aa2cbc2", "5b70cd567551d03d4c43c5cec9e02d1a");

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Flutter Demo',
        theme: ThemeData(
          canvasColor: Colors.white,
          primarySwatch: Colors.pink,
        ),
        home: MyHomePage(openApi, title: 'Flutter Demo Home Page'),
      );
}

class MyHomePage extends StatefulWidget {
  MyHomePage(this.api, {Key key, this.title}) : super(key: key);

  final String title;
  final KK.KKBOXOpenAPI api;
  final GlobalKey<ScaffoldState> scaffoldKey = GlobalKey<ScaffoldState>();

  @override
  MyHomePageState createState() => MyHomePageState(api);
}

class MyHomePageState extends State<MyHomePage> with TickerProviderStateMixin {
  final nowPlayingHeight = 60.0;
  int _pageIndex = 0;
  KK.KKBOXOpenAPI api;
  Map<String, dynamic> trackInfoMap;
  bool isPlaying = false;
  var tabs = <BottomBarTab>[
    BottomBarTab(
      initPage: DiscoverPage(MyApp.openApi),
      tabIconBuilder: (_) => Icon(Icons.whatshot),
      tabTitleBuilder: (_) => Text("Featured"),
    ),
    BottomBarTab(
      initPage: CategoryPage(MyApp.openApi),
      tabIconBuilder: (_) => Icon(Icons.library_music),
      tabTitleBuilder: (_) => Text("Playlist"),
    ),
  ];

  MyHomePageState(this.api);

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
  Widget build(BuildContext context) => MultiNavigatorBottomBar(
        initTabIndex: _pageIndex,
        tabs: tabs,
        pageWidgetDecorator: (pageWidget) => Column(
              children: <Widget>[
                Expanded(child: pageWidget),
                trackInfoMap != null
                    ? buildNowPlayingBar(
                        trackInfoMap['coverUrl'],
                        trackInfoMap['name'],
                        trackInfoMap['artistName'],
                        isPlaying)
                    : Container(height: 0.0),
              ],
            ),
      );

  Widget buildNowPlayingBar(
      String coverUrl, String trackName, String artistName, bool isPlaying) {
    return GestureDetector(
      onTap: () => KubePlayerPlugin.openNowPlaying(),
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
            IconButton(
              iconSize: nowPlayingHeight - 32.0,
              icon: Icon(
                isPlaying ? Icons.pause : Icons.play_arrow,
                color: Colors.white,
              ),
              onPressed: () {
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
