import 'package:easy_listview/easy_listview.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_kube/DiscoverDetailPage.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:kube_player_plugin/kube_player_plugin.dart';
import 'package:transparent_image/transparent_image.dart';

class DiscoverPage extends StatefulWidget {
  DiscoverPage(this.api, {Key key}) : super(key: key);

  final String title = "Discover";
  final KK.KKBOXOpenAPI api;

  @override
  _DiscoverPageState createState() => new _DiscoverPageState();
}

class _DiscoverPageState extends State<DiscoverPage> {
  List<KK.PlaylistInfo> playlistInfoList = [];
  var scrollController = ScrollController();
  var appBarBuilder = (BuildContext context, bool innerBoxIsScrolled) => [
        SliverAppBar(
          expandedHeight: 120.0,
          pinned: true,
          automaticallyImplyLeading: false,
          backgroundColor: Colors.white,
          flexibleSpace: FlexibleSpaceBar(
            title: Container(
              child: Text(
                "Today",
                style: TextStyle(color: Colors.black),
              ),
            ),
          ),
        ),
      ];

  @override
  Widget build(BuildContext context) => bodyWidget();

  Widget bodyWidget() {
    if (playlistInfoList.length == 0) {
      return FutureBuilder<KK.PlaylistList>(
        future: widget.api.fetchFeaturedPlaylists(),
        builder: (context, snapshot) {
          switch (snapshot.connectionState) {
            case ConnectionState.none:
              return tempLayout(const Text(""));
            case ConnectionState.waiting:
              return tempLayout(const Text("Loading..."));
            default:
              if (snapshot.hasError) {
                return tempLayout(Text('Error: ${snapshot.error}'));
              } else {
                playlistInfoList.addAll(snapshot.data.playlists);
              }
          }
          return buildList(playlistInfoList);
        },
      );
    } else {
      return buildList(playlistInfoList);
    }
  }

  tempLayout(Widget child) {
    return Scaffold(
      body: NestedScrollView(
        headerSliverBuilder: appBarBuilder,
        body: Container(
          alignment: AlignmentDirectional.center,
          child: child,
        ),
      ),
    );
  }

  Widget buildList(List<KK.PlaylistInfo> playlistInfoList) {
    var listItemBuilder = (BuildContext context, int index) {
      var playlistInfo = playlistInfoList[index];
      var screenWidth = MediaQuery.of(context).size.width;
      var heroTag = "item avatar$index";
      var navigationDuration =
          CupertinoPageRoute(builder: (_) {}).transitionDuration;
      return GestureDetector(
        onTap: () => Navigator.push(context, CupertinoPageRoute(builder: (_) {
              return DiscoverDetailPage(
                widget.api,
                playlistInfo: playlistInfo,
                heroTag: heroTag,
                navigationDuration: navigationDuration,
              );
            })),
        child: Column(
          children: [
            CoverWithPlayButtonWidget(
              screenWidth: screenWidth,
              heroTag: heroTag,
              playlistInfo: playlistInfo,
            ),
            Container(
              alignment: AlignmentDirectional.topStart,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16.0, 4.0, 80.0, 0.0),
                child: Text(
                  playlistInfo.title,
                  style: TextStyle(
                    fontSize: 28.0,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
            Container(
              alignment: AlignmentDirectional.topStart,
              child: Padding(
                padding: const EdgeInsets.only(left: 16.0, right: 16.0),
                child: Row(
                  children: [
                    Text(
                      "作者: ",
                      style: TextStyle(fontSize: 18.0, color: Colors.black87),
                    ),
                    Text(
                      playlistInfo.owner.name,
                      style: TextStyle(
                          fontSize: 18.0,
                          color: Colors.black87,
                          decoration: TextDecoration.underline),
                    )
                  ],
                ),
              ),
            ),
            Container(
              alignment: AlignmentDirectional.topStart,
              child: Padding(
                padding: const EdgeInsets.only(left: 16.0, right: 16.0),
                child: Text(
                  playlistInfo.description,
                  style: TextStyle(fontSize: 14.0, color: Colors.black87),
                  overflow: TextOverflow.ellipsis,
                  maxLines: 3,
                ),
              ),
            ),
            Container(
              alignment: AlignmentDirectional.topStart,
              child: Padding(
                padding: const EdgeInsets.only(
                    left: 16.0, right: 16.0, bottom: 16.0, top: 4.0),
                child: Text(
                  playlistInfo.lastUpdateDate,
                  style: TextStyle(fontSize: 14.0, color: Colors.black45),
                ),
              ),
            ),
          ],
        ),
      );
    };

    return Scaffold(
      body: EasyListView(
        itemCount: playlistInfoList.length,
        headerSliverBuilder: appBarBuilder,
        itemBuilder: listItemBuilder,
        dividerSize: 1.0,
      ),
    );
  }
}

class CoverWithPlayButtonWidget extends StatelessWidget {
  const CoverWithPlayButtonWidget({
    Key key,
    @required this.screenWidth,
    @required this.heroTag,
    @required this.playlistInfo,
    this.playButtonSize = 54.0,
  }) : super(key: key);

  final double screenWidth;
  final String heroTag;
  final double playButtonSize;
  final KK.PlaylistInfo playlistInfo;

  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment:
          Alignment(0.9, 1.0 + 1.0 / (screenWidth / playButtonSize) + 0.015),
      children: [
        Container(
          width: screenWidth,
          height: screenWidth,
          child: Stack(
            children: [
              Container(color: Colors.black26),
              Hero(
                tag: heroTag,
                child: FadeInImage.memoryNetwork(
                  fadeInDuration: Duration(milliseconds: 200),
                  placeholder: kTransparentImage,
                  image: playlistInfo.images[2].url,
                  fit: BoxFit.cover,
                ),
              ),
            ],
          ),
        ),
        Container(
          child: ButtonTheme(
            height: playButtonSize,
            minWidth: playButtonSize,
            child: RaisedButton(
              color: Colors.pinkAccent,
              onPressed: () => KubePlayerPlugin.startPlay(playlistInfo.id)
                  .then((success) {}),
              padding: EdgeInsets.all(0.0),
              child: Icon(
                Icons.play_arrow,
                color: Colors.white,
                size: 36.0,
              ),
            ),
          ),
        ),
      ],
    );
  }
}
