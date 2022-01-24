class FeedPost {

  FeedPost(this.id, this.userId, this.username, this.imgType, this.description, this.tags, this.postedAt, this.lastUpdateAt);

  String id;
  String userId;
  String username;
  String imgType;
  String description;
  List<dynamic> tags;
  num postedAt;
  num lastUpdateAt;

  FeedPost.fromJson(Map<String, dynamic> json)
      : id = json['id'],
        userId = json['userId'],
        username = json['username'],
        imgType = json['imgType'],
        description = json['description'],
        tags = json['tags'],
        postedAt = json['postedAt'],
        lastUpdateAt = json['lastUpdateAt'];

}