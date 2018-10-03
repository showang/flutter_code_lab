import 'package:easy_listview/easy_listview.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter/material.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:transparent_image/transparent_image.dart';

class CategoryPage extends StatefulWidget {
  CategoryPage(this.api, {Key key}) : super(key: key);

  final KK.KKBOXOpenAPI api;

  @override
  CategoryPageState createState() => CategoryPageState();
}

class CategoryPageState extends State<CategoryPage> {
  var playlistInfoList = <KK.PlaylistInfo>[];
  var appBarBuilder = (BuildContext context, bool innerBoxIsScrolled) =>
  [
    SliverAppBar(
      expandedHeight: 120.0,
      pinned: true,
      automaticallyImplyLeading: false,
      backgroundColor: Colors.white,
      flexibleSpace: FlexibleSpaceBar(
        title: Container(
          child: Text(
            "歌單",
            style: TextStyle(color: Colors.black),
          ),
        ),
      ),
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: NestedScrollView(
        headerSliverBuilder: appBarBuilder,
        body: Container(
          child: bodyWidget(),
          alignment: AlignmentDirectional.center,
        ),
      ),
    );
  }

  Widget bodyWidget() {
    if (playlistInfoList.length == 0) {
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
                playlistInfoList.addAll(snapshot.data.playlists);
              return buildList(playlistInfoList);
          }
        },
      );
    } else {
      return buildList(playlistInfoList);
    }
  }

  Widget buildList(List<KK.PlaylistInfo> playlistList) {
    var imageHeight = 100.0;
    var screenWidth = MediaQuery
        .of(context)
        .size
        .width;
    var listItemBuilder = (BuildContext context, int index) {
      var playlistInfo = playlistList[index];
      return Container(
        height: imageHeight + 1,
        width: screenWidth,
        padding: EdgeInsets.fromLTRB(28.0, 0.0, 28.0, 0.0),
        child: Container(
          decoration:
          BoxDecoration(border: Border.all(color: Colors.grey, width: 0.5)),
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
                    image: playlistInfo.images[2].url,
                    fadeInDuration: Duration(milliseconds: 200),
                  )
                ],
              ),
              Container(
                padding: EdgeInsets.all(16.0),
                alignment: AlignmentDirectional.center,
                child: Text(
                  playlistInfo.title,
                  style: TextStyle(
                    fontSize: 22.0,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              )
            ],
          ),
        ),
      );
    };

    return MediaQuery.removePadding(
      context: context,
      removeTop: true,
      child: EasyListView(
        itemCount: playlistInfoList.length,
        dividerSize: 16.0,
        dividerColor: Colors.transparent,
        itemBuilder: listItemBuilder,
        physics: BouncingScrollPhysics(),
      ),
    );
  }
}
