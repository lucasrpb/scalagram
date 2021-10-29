import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

class Session {

  Map<String, String> headers = {};

  Map<String, String> cookies = {};

  Future<http.Response> get(String url, Map<String, String> h) async {
    http.Response response = await http.get(Uri.parse(url), headers: h);
    updateCookie(response);
    return response;
  }

  Future<http.Response> post(String url, dynamic data, Map<String, String> h) async {
    http.Response response = await http.post(Uri.parse(url), body: data, headers: h);
    updateCookie(response);
    return response;
  }

  void updateCookie(http.Response response) async {

    String? rawCookie = response.headers['set-cookie'];

    /*print(response.statusCode);
    print(headers);
    print(response.body);*/

    if (rawCookie != null) {
      int index = rawCookie.indexOf(';');
      headers['cookie'] =
      (index == -1) ? rawCookie : rawCookie.substring(0, index);
    }

  }

  void _setCookie(String rawCookie) {
    if (rawCookie.length > 0) {
      var keyValue = rawCookie.split('=');
      if (keyValue.length == 2) {
        var key = keyValue[0].trim();
        var value = keyValue[1];

        // ignore keys that aren't cookies
        if (key == 'path' || key == 'expires')
          return;

        this.cookies[key] = value;
      }
    }
  }

  void _updateCookie(http.Response response) {
    String? allSetCookie = response.headers['set-cookie'];

    if (allSetCookie != null) {

      var setCookies = allSetCookie.split(',');

      for (var setCookie in setCookies) {
        var cookies = setCookie.split(';');

        for (var cookie in cookies) {
          _setCookie(cookie);
        }
      }

      headers['cookie'] = _generateCookieHeader();
    }
  }

  String _generateCookieHeader() {
    String cookie = "";

    for (var key in cookies.keys) {
      if (cookie.length > 0)
        cookie += ";";
      cookie += key + "=${cookies[key]}";
    }

    return cookie;
  }
}