import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_kube/DiscoverDetailPage.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;

class DiscoverPage extends StatefulWidget {
  DiscoverPage(this.api, {Key key}) : super(key: key);

  final String title = "Discover";
  final KK.KKBOXOpenAPI api;
  final List<KK.PlaylistInfo> playlistInfoList = [];

  @override
  _DiscoverPageState createState() => new _DiscoverPageState();
}

class _DiscoverPageState extends State<DiscoverPage> {
  ScrollController scrollController =
      new ScrollController(keepScrollOffset: true);

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
      appBar: new AppBar(
        title: new Text(
          "Discover",
          style: new TextStyle(color: Colors.black),
        ),
        backgroundColor: Colors.white,
        elevation: 0.0,
      ),
      body: new Container(child: bodyWidget()),
    );
  }

  Widget bodyWidget() {
    if (widget.playlistInfoList.length == 0) {
      return new FutureBuilder<KK.PlaylistList>(
        future: widget.api.fetchFeaturedPlaylists(),
        builder: (context, snapshot) {
          switch (snapshot.connectionState) {
            case ConnectionState.none:
              return new Text("");
            case ConnectionState.waiting:
              return new Text("Loading...");
            default:
              if (snapshot.hasError)
                return new Text('Error: ${snapshot.error}');
              else
                widget.playlistInfoList.addAll(snapshot.data.playlists);
              return buildList(widget.playlistInfoList);
          }
        },
      );
    } else {
      return buildList(widget.playlistInfoList);
    }
  }

  Widget buildList(List<KK.PlaylistInfo> playlistInfoList) {
    return RefreshIndicator(
      onRefresh: () {
        new Timer(const Duration(milliseconds: 3000), () {
          setState(() {
            //TODO Refresh
          });
        });
      },
      child: ListView.builder(
        controller: scrollController,
        itemCount: playlistInfoList.length * 2,
        itemBuilder: (BuildContext context, int index) {
          if (index.isOdd)
            return const Divider(
              height: 1.0,
              color: Colors.black12,
            );

          int itemIndex = index ~/ 2;
          var playlistInfo = playlistInfoList[itemIndex];
          return new ListTile(
            leading: Hero(
              tag: "item avatar$itemIndex",
              child: Image.network(
                playlistInfoList[itemIndex].images[1].url,
                fit: BoxFit.cover,
                width: 60.0,
              ),
            ),
            title: new Text(playlistInfo.title),
            subtitle: new Text(playlistInfo.owner.name),
            contentPadding: EdgeInsets.symmetric(horizontal: 16.0),
            onTap: () {
              var page = DiscoverDetailPage(widget.api,
                  playlistInfo: playlistInfo, heroTag: "item avatar$itemIndex");

              Navigator.push(context, CupertinoPageRoute(builder: (_) {
                return page;
              }));
            },
          );
        },
      ),
    );
  }
}
