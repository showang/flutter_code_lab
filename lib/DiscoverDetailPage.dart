import 'package:easy_listview/easy_listview.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:after_routing_handler/after_routing_handler.dart';
import 'package:kkbox_openapi/kkbox_openapi.dart' as KK;
import 'package:kube_player_plugin/kube_player_plugin.dart';
import 'package:share/share.dart';
import 'package:transparent_image/transparent_image.dart';
import 'package:url_launcher/url_launcher.dart';

class DiscoverDetailPage extends StatefulWidget {
  DiscoverDetailPage(this.api,
      {this.playlistInfo, this.heroTag, this.navigationDuration});

  final String heroTag;
  final KK.PlaylistInfo playlistInfo;
  final KK.KKBOXOpenAPI api;
  final Duration navigationDuration;

  @override
  State<StatefulWidget> createState() => DiscoverDetailState(heroTag);
}

enum TrackOptions { openInKkbox, playTrack }

class DiscoverDetailState extends State<DiscoverDetailPage> {
  DiscoverDetailState(this.heroTag) {
    scrollController = ScrollController()
      ..addListener(() {
        var isScrollDown = scrollController.offset > _lastOffset;
        if (isScrollDown && _actionButtonVisible)
          setState(() => _actionButtonVisible = false);
        else if (!isScrollDown && !_actionButtonVisible)
          setState(() => _actionButtonVisible = true);
        _lastOffset = scrollController.offset;
      });
  }

  String heroTag;
  bool hasNextPage = true;
  int nextOffset = 0;
  List<KK.TrackInfo> tracks = [];
  var expandDesc = false;
  var _lastOffset = 0.0;
  var _actionButtonVisible = true;
  ScrollController scrollController;

  KK.PlaylistInfo get playlistInfo => widget.playlistInfo;

  Future<KK.TrackList> apiFuture(offset) =>
      widget.api.fetchTracksInPlaylist(playlistInfo.id, offset: offset);

  @override
  void initState() {
    super.initState();
    AfterRoutingHandler(pageState: this, duration: widget.navigationDuration)
      ..apiUpdate(
        fetchData: tracks.length == 0,
        apiFuture: apiFuture(0),
        apiErrorCallback: (e) => print("onApiError: $e"),
        updateDataDelegate: _updateTrackList,
      );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: EasyListView(
        itemCount: tracks.length,
        itemBuilder: _itemBuilder,
        headerBuilder: _headerViewBuilder,
        loadMore: hasNextPage,
        onLoadMore: () => requestPageData(nextOffset),
        loadMoreWhenNoData: true,
        dividerBuilder: (context, index) => Divider(height: 1.0, color: Colors.grey,),
        controller: scrollController,
      ),
      floatingActionButton: AnimatedOpacity(
        duration: Duration(milliseconds: 200),
        opacity: _actionButtonVisible ? 1.0 : 0.0,
        child: FloatingActionButton(
            child: Icon(
              Icons.play_arrow,
              color: Colors.white,
            ),
            onPressed: () => KubePlayerPlugin.startPlay(playlistInfo.id)),
      ),
    );
  }

  void requestPageData(int offset) {
    widget.api
        .fetchTracksInPlaylist(playlistInfo.id, offset: offset)
        .then(_updateTrackList);
  }

  _updateTrackList(KK.TrackList trackList) => setState(() {
        hasNextPage = trackList.tracks.length > 0;
        tracks.addAll(trackList.tracks);
        nextOffset = tracks.length;
      });

  Widget buildCoverImage() {
    var screenWidth = MediaQuery.of(context).size.width;
    return GestureDetector(
      child: Container(
        width: screenWidth,
        height: screenWidth,
        child: Hero(
          tag: heroTag,
          child: Image.network(
            playlistInfo.images[1].url,
            fit: BoxFit.fitWidth,
          ),
        ),
      ),
      onTap: () {
        Navigator.pop(context);
      },
    );
  }

  _launchURL(String url) async {
    if (await canLaunch(url)) {
      await launch(url);
    } else {
      throw 'Could not launch $url';
    }
  }

  Widget _itemBuilder(BuildContext context, int index) {
    var track = tracks[index];
    return ListTile(
      contentPadding: EdgeInsets.only(left: 8.0, right: 8.0),
      title: Text(track.name),
      subtitle: Text(track.album.artist.name),
      leading: Image.network(
        track.album.images[0].url,
        fit: BoxFit.cover,
        width: 60.0,
      ),
      trailing: PopupMenuButton<TrackOptions>(
        icon: Icon(Icons.more_vert),
        onSelected: (TrackOptions result) {
          switch (result) {
            case TrackOptions.playTrack:
              KubePlayerPlugin.startPlay(playlistInfo.id, index);
              break;
            case TrackOptions.openInKkbox:
              _launchURL(tracks[index].url);
              break;
          }
        },
        itemBuilder: (BuildContext context) => <PopupMenuEntry<TrackOptions>>[
              const PopupMenuItem<TrackOptions>(
                value: TrackOptions.playTrack,
                child: const Text('Play track'),
              ),
              const PopupMenuItem<TrackOptions>(
                value: TrackOptions.openInKkbox,
                child: const Text('Open in KKBOX'),
              ),
            ],
      ),
      onTap: () {
        KubePlayerPlugin.startPlay(playlistInfo.id, index).then((success) {});
      },
    );
  }

  Widget _headerViewBuilder(BuildContext context) {
    var screenWidth = MediaQuery.of(context).size.width;
    return Column(children: <Widget>[
      Container(
        padding: EdgeInsets.only(left: 8.0, top: 8.0, right: 8.0, bottom: 12.0),
        alignment: AlignmentDirectional.topStart,
        child: Stack(
          alignment: Alignment(1.0, 1.0),
          children: [
            Column(
              children: <Widget>[
                Container(
                  padding: EdgeInsets.only(
                      left: 0.0, top: 0.0, right: 0.0, bottom: 4.0),
                  alignment: AlignmentDirectional.topStart,
                  child: Text(
                    playlistInfo.title,
                    style:
                        TextStyle(fontSize: 32.0, fontWeight: FontWeight.bold),
                    textAlign: TextAlign.start,
                  ),
                ),
                Row(
                  children: <Widget>[
                    Text(
                      "作者：",
                      style: TextStyle(fontSize: 18.0),
                    ),
                    Text(playlistInfo.owner.name,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 18.0,
                          decoration: TextDecoration.underline,
                        )),
                  ],
                ),
              ],
            ),
            Container(
              alignment: AlignmentDirectional.topEnd,
              child: IconButton(
                onPressed: () => Share.share(
                    "https://kube-app.com/playlist/${playlistInfo.id}"),
                icon: Icon(
                  Icons.share,
                  size: 36.0,
                ),
                color: Colors.black54,
              ),
            ),
          ],
        ),
      ),
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
        padding:
            EdgeInsets.only(left: 16.0, top: 8.0, right: 16.0, bottom: 0.0),
        alignment: AlignmentDirectional.topStart,
        child: Text(
          "更新時間：${playlistInfo.lastUpdateDate}",
          style: TextStyle(
            fontSize: 18.0,
            color: Colors.black45,
          ),
        ),
      ),
      Container(
        padding:
            EdgeInsets.only(left: 16.0, top: 8.0, right: 16.0, bottom: 8.0),
        alignment: AlignmentDirectional.topStart,
        child: GestureDetector(
          onTap: () => setState(() => expandDesc = !expandDesc),
          child: Text(
            playlistInfo.description,
            maxLines: expandDesc ? 20 : 5,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 18.0,
            ),
          ),
        ),
      ),
    ]);
  }
}
