# Emoji Translator
A simple Android app that allows you to get the emoji equivalent of your face expressions

Emoji Translator uses the new Mobile Vision API from Google to classify facial expressions and convert them to a corresponding emoji. The "Classification" feature in the API is used for determining the expression. The emojis names are used to identify their matching expressions like for example: :grinning: (Grinning).


### Current limitations
At the moment, the Mobile Vision API supports only two classifications: eyes open and smiling. An alternative that is being considering for detecting a wider array of expressions, is the use of OpenCv with a custom facial landmark detector algorithm.
