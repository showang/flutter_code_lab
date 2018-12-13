import 'package:after_routing_handler/after_routing_handler.dart';
import 'package:easy_listview/easy_listview.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_kube/PlaylistDetailPage.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:transparent_image/transparent_image.dart';

import 'generated/i18n.dart';

class PlaylistPage extends StatefulWidget {
  PlaylistPage(this.api, {Key key}) : super(key: key);

  final KK.KKBOXOpenAPI api;

  @override
  _PlaylistPageState createState() => _PlaylistPageState();
}

class _PlaylistPageState extends State<PlaylistPage> {
  var hotPlaylistInfoList = <KK.PlaylistInfo>[];
  var chartPlaylistInfoList = <KK.PlaylistInfo>[];
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
                S.of(context).playlist,
                style: TextStyle(color: Colors.black),
              ),
            ),
          ),
        ),
      ];

  @override
  void initState() {
    super.initState();
    AfterRoutingHandler(this)
      ..scheduleFuture<KK.PlaylistList>(
        widget.api.fetchNewHitsPlaylists(),
        shouldInvoke: hotPlaylistInfoList.isEmpty,
        errorCallback: (e) {},
        successDelegate: (playlistList) {
          setState(() {
            print("hotPlaylistInfoList loaded success");
            hotPlaylistInfoList.addAll(playlistList.playlists);
          });
        },
      )
      ..scheduleFuture(
        widget.api.fetchCharts(),
        shouldInvoke: chartPlaylistInfoList.isEmpty,
        errorCallback: (e) {},
        successDelegate: (playlistList) {
          setState(() {
            print("chartPlaylistInfoList loaded success");
            chartPlaylistInfoList.addAll(playlistList.playlists);
          });
        },
      );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
          body:
//          Text("test")
          EasyListView(
            headerSliverBuilder: appBarBuilder,
            itemCount: hotPlaylistInfoList.length + chartPlaylistInfoList.length,
            dividerBuilder: (context, index) => Container(height: 16.0),
            itemBuilder: listItemBuilder(),
      )
//        NestedScrollView(
//          headerSliverBuilder: appBarBuilder,
//          body: Container(
//            child: bodyWidget(),
//            alignment: AlignmentDirectional.center,
//          ),
//        ),
          );

  Widget bodyWidget() {
    if (chartPlaylistInfoList.length == 0) {
      return FutureBuilder<KK.PlaylistList>(
        future: widget.api.fetchCharts(),
        builder: (context, snapshot) {
          switch (snapshot.connectionState) {
            case ConnectionState.none:
              return Text("");
            case ConnectionState.waiting:
              return const Text("Loading...");
            default:
              if (snapshot.hasError)
                return Text('Error: ${snapshot.error}');
              else
                chartPlaylistInfoList.addAll(snapshot.data.playlists);
              return buildList();
          }
        },
      );
    } else {
      return buildList();
    }
  }

  IndexedWidgetBuilder listItemBuilder() {
    var imageHeight = 100.0;
    var screenWidth = MediaQuery.of(context).size.width;
    return (BuildContext context, int index) {
      KK.PlaylistInfo playlistInfo;
      var hotPlaylistIndex = index - hotPlaylistInfoList.length;
      if (index < hotPlaylistInfoList.length) {
        playlistInfo = hotPlaylistInfoList[index];
      } else if (hotPlaylistIndex > 0 &&
          hotPlaylistIndex < chartPlaylistInfoList.length) {
        playlistInfo = chartPlaylistInfoList[hotPlaylistIndex];
      }
      if (playlistInfo == null) return Container();
      return Container(
        height: imageHeight + 0.5 * 2,
        width: screenWidth,
        padding: EdgeInsets.fromLTRB(28.0, 0.0, 28.0, 0.0),
        child: GestureDetector(
          onTap: () => openPlaylistDetailPage(playlistInfo, index),
          child: Container(
            decoration: BoxDecoration(
                border: Border.all(color: Colors.grey, width: 0.5)),
            child: Row(
              children: <Widget>[
                Stack(
                  children: <Widget>[
                    Container(
                      height: imageHeight,
                      width: imageHeight,
                      color: Colors.black26,
                    ),
                    FadeInImage.memoryNetwork(
                      fit: BoxFit.cover,
                      placeholder: kTransparentImage,
                      image: playlistInfo.images[0].url,
                      fadeInDuration: Duration(milliseconds: 200),
                    )
                  ],
                ),
                Flexible(
                  child: Container(
                    padding: EdgeInsets.all(16.0),
                    alignment: AlignmentDirectional.center,
                    child: Text(
                      playlistInfo.title,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 22.0,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                )
              ],
            ),
          ),
        ),
      );
    };
  }

  Widget buildList() {


    return MediaQuery.removePadding(
      context: context,
      removeTop: true,
      child: EasyListView(
        itemCount: hotPlaylistInfoList.length + chartPlaylistInfoList.length,
        dividerBuilder: (context, index) => Container(height: 16.0),
        itemBuilder: listItemBuilder(),
      ),
    );
  }

  openPlaylistDetailPage(KK.PlaylistInfo playlistInfo, int index) {
    var heroTag = "CategoryItem:$index";
    var navigationDuration = const Duration(milliseconds: 350);
    var page = PlaylistDetailPage(
      widget.api,
      playlistInfo: playlistInfo,
      heroTag: heroTag,
      navigationDuration: navigationDuration,
    );
    var route = CupertinoPageRoute(builder: (_) => page);
    Navigator.push(
      context,
      route,
    );
  }
}
