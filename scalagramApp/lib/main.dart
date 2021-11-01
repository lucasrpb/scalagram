
import 'package:scalagramApp/login_page.dart';
import 'package:flutter/material.dart';

import "session.dart";
import "navbar.dart";

void main() async {
  runApp(const MyApp());
}

/// This is the main application widget.
class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  static const String _title = 'Scalagram';

  @override
  Widget build(BuildContext context) {

    Session session = new Session();

    return MaterialApp(
      //color: Colors.deepPurpleAccent,
      title: _title,
      home: Scaffold(
        backgroundColor: Colors.amberAccent,
        appBar: AppBar(
         // backgroundColor: Colors.deepPurpleAccent,
          title: const Text(_title),
        ),
        body:  LoginPageWidget(session: session, URL: "http://10.0.2.2:9000/users/login"),
      ),
    );
  }
}
