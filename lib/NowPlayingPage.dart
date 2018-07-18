import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';

class NowPlayingPage extends StatefulWidget {
  NowPlayingPage(this.bottomSheetController);

  final PersistentBottomSheetController bottomSheetController;

  @override
  State<StatefulWidget> createState() {
    return new NowPlayingState();
  }
}

class NowPlayingState extends State<NowPlayingPage> {
  @override
  Widget build(BuildContext context) {
    return new Container(
      constraints: BoxConstraints.loose(Size.fromHeight(60.0)),
      decoration: new BoxDecoration(color: Colors.blueGrey),
      child: new Row(
        children: <Widget>[
          Image.network(
            "https://i.kfs.io/playlist/global/62071022v1/cropresize/300x300.jpg",
            alignment: Alignment.topCenter,
          ),
          new Text("Song Name"),
          new RaisedButton(
            child: new Text("Test"),
            onPressed: () {
              widget.bottomSheetController.setState((){

              });
            },
          )
        ],
      ),
    );
  }
}
