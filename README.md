**SCALAGRAM**

to build: 

docker build -t scalagram .

to run: 

docker run --name scalagram-app -p 9000:9000 scalagram

Don't forget to copy root.crt and google_cloud_credentials.json to dist folder in target/universal...

Deployment steps on gcp: 

1. cd into the project directory
2. docker build -t gcr.io/scalable-services/scalagram .
3. docker push gcr.io/scalable-services/scalagram 
4. create service on Google Cloud Run and select the image:



