const { throws } = require('assert');
const { error } = require('console');
const https = require('https');

const hereApiKey = process.env.HERE_API_KEY;

 async function getRoute(location, destination) {
     return  new Promise((resolve, reject) => {
        const options = {
            host: 'router.hereapi.com',
            path: '/v8/routes?transportMode=car&origin=' + location + '&destination=' + destination + '&return=summary&apiKey=' + hereApiKey,
            method: 'GET',
        };

        https.request(options, (res) => {
            let data='';
            res.on('data', function (chunk) {
                data += chunk;
            });
            res.on('end', () => {
                const route = JSON.parse(data).routes[0].sections[0].summary;
                resolve(route);
            });
        }).on('error', (error) => {
            reject(error);
        }).end();
    })
}
module.exports = { getRoute };