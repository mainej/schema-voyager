const purgecss = require('@fullhuman/postcss-purgecss')({
    content: [
        './assets/index.html',
        './src/**/*.cljs',
        './src/**/*.cljc',
    ],

    // Include any special characters you're using in this regular expression
    defaultExtractor: content => content.match(/[\w-][\w-:]*/g) || []
});

module.exports = {
    plugins: [
        require('postcss-import'),
        require('tailwindcss'),
        require('postcss-preset-env')({ stage: 1 }),
        ...process.env.NODE_ENV === 'production'
            ? [purgecss]
            : []
    ]
};
