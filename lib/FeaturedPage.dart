import 'package:easy_listview/easy_listview.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_kube/KubePlayerPlugin.dart';
import 'package:flutter_kube/PlaylistDetailPage.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:transparent_image/transparent_image.dart';

import 'generated/i18n.dart';

class FeaturedPage extends StatefulWidget {
  FeaturedPage(this.api, {Key key}) : super(key: key);

  final KK.KKBOXOpenAPI api;
  final GlobalKey<ScaffoldState> discoverScaffoldKey =
      GlobalKey<ScaffoldState>();

  @override
  _FeaturedPageState createState() => _FeaturedPageState();

  static String title() => _FeaturedPageState.title;
}

class _FeaturedPageState extends State<FeaturedPage> {
  _FeaturedPageState();

  List<KK.PlaylistInfo> playlistInfoList = [];
  static String title = "今日精選";
  var scrollController = ScrollController();
  var appBarBuilder = (BuildContext context, bool innerBoxIsScrolled) => [
        SliverAppBar(
          expandedHeight: 120.0,
          pinned: true,
          brightness: Brightness.light,
          automaticallyImplyLeading: false,
          backgroundColor: Colors.white,
          flexibleSpace: FlexibleSpaceBar(
            title: Container(
              child: Text(
                S.of(context).featured,
                style: TextStyle(color: Colors.black),
              ),
            ),
          ),
        ),
      ];

  @override
  Widget build(BuildContext context) => playlistInfoList.length == 0
      ? loadingFutureBuilder()
      : buildList();

  loadingFutureBuilder() => FutureBuilder<KK.PlaylistList>(
        future: widget.api.fetchFeaturedPlaylists(),
        builder: (context, snapshot) {
          Widget foreground;
          switch (snapshot.connectionState) {
            case ConnectionState.none:
              foreground = tempLayout(const Text(""));
              break;
            case ConnectionState.waiting:
              foreground = tempLayout(const Text("Loading..."));
              break;
            default:
              if (snapshot.hasError) {
                foreground = tempLayout(Text('Error: ${snapshot.error}'));
              } else {
                playlistInfoList.addAll(snapshot.data.playlists);
              }
          }
          return buildList(foreground);
        },
      );

  Widget tempLayout(Widget child) =>
      Container(alignment: AlignmentDirectional.center, child: child);

  Widget buildList([Widget foreground]) {
    return Scaffold(
      key: widget.discoverScaffoldKey,
      body: foreground == null ? EasyListView(
        foregroundWidget: foreground,
        itemCount: playlistInfoList.length,
        headerSliverBuilder: appBarBuilder,
        itemBuilder: listItemBuilder(),
        dividerBuilder: (context, index) => Divider(
              height: 1.0,
              color: Colors.grey,
            ),
      ) : foreground,
    );
  }


  listItemBuilder() => (BuildContext context, int index) {
    if(playlistInfoList.length == 0) return Text("No data");
    var playlistInfo = playlistInfoList[index];
    var screenWidth = MediaQuery.of(context).size.width;
    var heroTag = "item avatar$index";
    var navigationDuration = const Duration(milliseconds: 350);
    return GestureDetector(
      onTap: () => Navigator.push(context, CupertinoPageRoute(builder: (_) {
        return PlaylistDetailPage(
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
                    S.of(context).title_author,
                    style: const TextStyle(
                        fontSize: 18.0, color: Colors.black87),
                  ),
                  Text(
                    playlistInfo.owner.name,
                    style: const TextStyle(
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
                style: const TextStyle(fontSize: 14.0, color: Colors.black87),
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
                style: const TextStyle(fontSize: 14.0, color: Colors.black45),
              ),
            ),
          ),
        ],
      ),
    );
  };
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
              padding: const EdgeInsets.all(0.0),
              child: const Icon(
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
