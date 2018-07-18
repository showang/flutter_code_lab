import 'package:flutter/widgets.dart';
import 'package:flutter/material.dart';

class CategoryPage extends StatefulWidget {
  CategoryPage({Key key}) : super(key: key);

  @override
  CategoryPageState createState() => CategoryPageState();
}

class CategoryPageState extends State<CategoryPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: new Center(
          child: new Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          new Text("Second Page"),
          new MaterialButton(
            color: Colors.pinkAccent,
            textColor: Colors.white,
            child: new Text("Snackbar!!!"),
            height: 48.0,
            onPressed: () {
              final snackBar = SnackBar(content: Text('Start Playing Music!!'));
              Scaffold.of(context).showSnackBar(snackBar);
            },
          ),
        ],
      )),
    );
  }
}
