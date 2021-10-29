import 'dart:io';

import 'package:scalagramApp/login_page.dart';
import 'package:flutter/material.dart';

class NavBar extends StatelessWidget {

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: ListView(
        children: [
          UserAccountsDrawerHeader(
            currentAccountPicture: CircleAvatar(
              child: Image.network(
                "http://10.0.2.2:9000/assets/images/me2.jpg",
                height: 90,
                width: 90,
                fit: BoxFit.cover,
              ),
            ),
            accountName: Text("lucasrpb"), 
            accountEmail: Text("lucas@gmail.com")
          )
        ],
      ),
    );
  }

}