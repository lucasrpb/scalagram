import 'package:scalagramApp/login_page.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import 'dart:convert';
import 'dart:io';
import 'dart:async';
import "session.dart";
import "models/feed_model.dart";

/// This is the stateful widget that the main application instantiates.
class FeedWidget extends StatefulWidget {
  FeedWidget({required this.session, Key? key}) : super(key: key);

  Session session;

  @override
  State<FeedWidget> createState() => _FeedWidgetState(session: this.session);
}

/// This is the private State class that goes with MyStatefulWidget.
class _FeedWidgetState extends State<FeedWidget> {

  _FeedWidgetState({required this.session});

  Session session;

  List<int> top = <int>[];

  List<FeedPost> posts = [];
  var URL = Uri.parse("http://10.0.2.2:9000/feed/posts/0/2");

  /*Future<http.Response> action() async {
    var response = await session.post("http://10.0.2.2:9000/action", {}, session.headers);

    print(response.statusCode);
    print(response.body);
    print(session.headers);

    assert(response.statusCode == 200);

    return response;
  }*/

  Future<List<FeedPost>> _getPosts() async {

    var response = await session.get("http://10.0.2.2:9000/feed/posts/${posts.length}/3", session.headers);
    
    if(response.statusCode != 200){

      print("session headers: ${session.headers}\n");
      print('Response status: ${response.statusCode}');
      print("${response.body}");

      return Future.value(List.empty());
    }

    //var decoded = json.decode('[{"id":"22693a11-b1fc-48cf-b2c5-6d78fa0b49dc","userId":"b684f957-e39d-456d-a7a5-dac0c5f723fd","imgType":"png","description":"best programming language ever !","tags":["scala","programming"],"postedAt":1635295083667},{"id":"bb322d70-2458-4fec-ba2a-df0574768c15","userId":"b684f957-e39d-456d-a7a5-dac0c5f723fd","imgType":"png","description":"best singer ever !","tags":["celine","dion"],"postedAt":1635294760641}]');
    var decoded = json.decode(response.body);
    List<FeedPost> list = [];

    for(var p in decoded){
      list.add(FeedPost.fromJson(p));
    }

    posts.addAll(list);

    print(list);

    setState(() {
    });

    return posts;
  }

  // This method will run once widget is loaded
  // i.e when widget is mounting
  @override
  void initState() {
    super.initState();

    _getPosts();

    /*_scrollController.addListener(() {

      _getPosts();
      //action();
      setState(() {

      });

    });*/
  }

  var _scrollController = new ScrollController();

  Future<void> update() async {

    setState(() {

    });

    return Future.value({});
  }

  @override
  Widget build(BuildContext context) {

    const Key centerKey = ValueKey<String>('bottom-sliver-list');
    return SafeArea(
        child: Scaffold(
            appBar: AppBar(
              title: const Text('Scalagram'),
              //backgroundColor: Colors.deepPurple,
              /*leading: IconButton(
              icon: const Icon(Icons.add),
              onPressed: () {
                setState(() {
                  top.add(-top.length - 1);
                });
              },
            ),*/
            ),
            body: Container(
              color: Colors.white,
              alignment: Alignment.center,
              child: RefreshIndicator(
                onRefresh: _getPosts,
                child: FutureBuilder(
                  future: update(),
                  builder: (BuildContext context, AsyncSnapshot snapshot) {

                    if(snapshot.connectionState != ConnectionState.done){

                      if(snapshot.hasError){
                        return Container(
                          child: Center(
                            child: Text("Something went terribly wrong!"),
                          ),
                        );
                      }

                      if(snapshot.connectionState == ConnectionState.active){
                        return Container(
                          child: Center(
                            child: Text("loading..."),
                          ),
                        );
                      }
                    }

                    //var posts = (snapshot.data as List<FeedPost>);

                    return  ListView.builder(
                      controller: _scrollController,
                      itemCount: posts.length,
                      itemBuilder: (BuildContext context, int index) {

                        var post = posts[index];

                        return card(post);
                      },
                    );
                  },
                ),
              ),
            )
        )
    );
  }

  Card card(FeedPost post) {
    return Card(
        margin: EdgeInsets.all(20),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(10.0),
        ),
        shadowColor: Colors.black,
        color: Colors.lightBlue,
        child: Container(
            child: Column(
              children: [
                Container(
                  child: Container(
                    alignment: Alignment.centerLeft,
                    margin: EdgeInsets.all(10.0),
                    child: Text(post.username, style: TextStyle(color: Colors.white)),
                  ),
                ),
                Container(
                  alignment: Alignment.center,
                  child: Image.network(
                    "http://10.0.2.2:9000/assets/pictures/${post.id}.${post.imgType}",
                    height: 200,
                    //width: 200,
                    fit: BoxFit.fitWidth,
                  ),
                ),
                Container(
                  child: Container(
                      margin: EdgeInsets.all(10.0),
                      child: Text(post.description, style: TextStyle(color: Colors.white, fontSize: 16.0),)
                  ),
                )
              ],
            )
        )/* ListTile(
                          title: Text(posts[index].description),
                        ),*/
    );
  }
}