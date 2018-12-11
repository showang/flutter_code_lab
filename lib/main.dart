import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_kube/PlaylistPage.dart';
import 'package:flutter_kube/FeaturedPage.dart';
import 'package:flutter_kube/KubePlayerPlugin.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:multi_navigator_bottom_bar/multi_navigator_bottom_bar.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

void main() => runApp(MyApp());

enum TabItem { discover, category }

class MyApp extends StatelessWidget {
  // This widget is the root of your application.

  static final KK.KKBOXOpenAPI openApi = KK.KKBOXOpenAPI(
      "a375efdf3d7e58762ae7a866f08af63d", "907640053e1a020c372602838d36f5c1");

  @override
  Widget build(BuildContext context) => MaterialApp(
        localizationsDelegates: [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate
        ],
        title: 'Flutter KUBE',
        theme: ThemeData(
          canvasColor: Colors.white,
          primarySwatch: Colors.pink,
        ),
        home: MyHomePage(openApi),
      );
}

class MyHomePage extends StatefulWidget {
  MyHomePage(this.api, {Key key}) : super(key: key);

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
      initPage: FeaturedPage(MyApp.openApi),
      tabIconBuilder: (_) => Icon(Icons.whatshot),
      tabTitleBuilder: (context) => Text("精選"),
    ),
    BottomBarTab(
      initPage: PlaylistPage(MyApp.openApi),
      tabIconBuilder: (_) => Icon(Icons.library_music),
      tabTitleBuilder: (context) => Text("歌單"),
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
  Widget build(BuildContext context) => Scaffold(
        body: pageContainer(context),
        bottomNavigationBar: BottomNavigationBar(
          currentIndex: _pageIndex,
          onTap: (index) => setState(() => _pageIndex = index),
          items: [
            BottomNavigationBarItem(
              icon: Icon(Icons.whatshot),
              title: Text("精選"),
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.library_music),
              title: Text("歌單"),
            ),
          ],
        ),
      );

  Widget pageContainer(BuildContext context) => Stack(children: <Widget>[
        Offstage(
          offstage: _pageIndex != 0,
          child: FeaturedPage(MyApp.openApi),
        ),
        Offstage(
          offstage: _pageIndex != 1,
          child: PlaylistPage(MyApp.openApi),
        ),
      ]);
}

class NowPlayingBar extends StatelessWidget {
  NowPlayingBar(
      {this.coverUrl, this.trackName, this.artistName, this.isPlaying});

  final nowPlayingHeight = 60.0;
  final String coverUrl;
  final String trackName;
  final String artistName;
  final bool isPlaying;

  @override
  Widget build(BuildContext context) => GestureDetector(
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
