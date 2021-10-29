import 'package:scalagramApp/navbar.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

import 'dart:convert';
import 'dart:io';
import 'dart:async';
import 'package:crypto/crypto.dart';
import 'feed.dart';
import "session.dart";

/// This is the stateful widget that the main application instantiates.
class LoginPageWidget extends StatefulWidget {

  Session session;
  String URL;

  LoginPageWidget({required this.session, required this.URL, Key? key}) : super(key: key);

  @override
  State<LoginPageWidget> createState() => LoginPageState(session: this.session, URL: this.URL);
}

class LoginPageState extends State<LoginPageWidget> {

  final _formKey = GlobalKey<FormState>();

  String login = "";
  String password = "";

  Session session;
  String URL;
  
  LoginPageState({required this.session, required this.URL});

  Future<http.Response> send(ScaffoldFeatureController sfc) async {
    var bytes = utf8.encode(this.password); // data being hashed
    var digest = sha1.convert(bytes).toString();

    var response = await session.post(URL, json.encode({
      "login": this.login,
      "password": digest
    }), {
      "content-type": "application/json",
      "login": this.login,
      "password": digest
    });

    print(response.statusCode);
    print(response.body);
    print(session.headers);

    if(response.statusCode == 200){

      sfc.close();

      Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => FeedWidget(session: this.session)),
      );
    }

    return response;
  }

  Future<http.Response> action() async {
    var response = await session.post("http://10.0.2.2:9000/action", {}, session.headers);

    print(response.statusCode);
    print(response.body);
    print(session.headers);

    return response;
  }

  @override
  Widget build(BuildContext context) {
    // Build a Form widget using the _formKey created above.
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          TextFormField(
            initialValue: "user0",
            // The validator receives the text that the user has entered.
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter some text';
              }

              login = value;

              return null;
            },
          ),
          TextFormField(
            initialValue: "4321",
            // The validator receives the text that the user has entered.
            validator: (value) {
              if (value == null || value.isEmpty) {
                return 'Please enter some text';
              }

              password = value;

              return null;
            },
          ),

          Padding(
            padding: const EdgeInsets.symmetric(vertical: 16.0),
            child: ElevatedButton(
              onPressed: () async {
                // Validate returns true if the form is valid, or false otherwise.
                if (_formKey.currentState!.validate()) {
                  // If the form is valid, display a snackbar. In the real world,
                  // you'd often call a server or save the information in a database.

                  var sfc = ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Processing data')),
                  );

                  send(sfc);
                }
              },
              child: const Text('Submit'),
            ),
          ),
          /*Padding(
            padding: const EdgeInsets.symmetric(vertical: 16.0),
            child: ElevatedButton(
              onPressed: () async {
                // Validate returns true if the form is valid, or false otherwise.
                if (_formKey.currentState!.validate()) {
                  // If the form is valid, display a snackbar. In the real world,
                  // you'd often call a server or save the information in a database.

                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Processing data')),
                  );
                }
              },
              child: const Text('Access'),
            ),
          ),*/
        ],
      ),
    );
  }
}