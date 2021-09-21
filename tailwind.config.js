const colors = require('tailwindcss/colors');

module.exports = {
    mode: 'jit',
    darkMode: false,
    theme: {
        extend: {
            colors: {
                teal: colors.teal,
            },
            spacing: {
                '0half': '0.125rem',
                '1half': '0.375rem',
                '2half': '0.625rem',
                '3half': '0.875rem',
                text: '1em',
            },
            width: {
                // originally 1/2, etc
                '1|2': '50%',
                '1|3': '33.333333%',
                '2|3': '66.666667%',
                '1|4': '25%',
                '2|4': '50%',
                '3|4': '75%',
                '1|5': '20%',
                '2|5': '40%',
                '3|5': '60%',
                '4|5': '80%',
                '1|6': '16.666667%',
                '2|6': '33.333333%',
                '3|6': '50%',
                '4|6': '66.666667%',
                '5|6': '83.333333%',
                '1|12': '8.333333%',
                '2|12': '16.666667%',
                '3|12': '25%',
                '4|12': '33.333333%',
                '5|12': '41.666667%',
                '6|12': '50%',
                '7|12': '58.333333%',
                '8|12': '66.666667%',
                '9|12': '75%',
                '10|12': '83.333333%',
                '11|12': '91.666667%',
            },
            translate: {
                '-1|2': '-50%',
                '1|2': '50%',
            },
            fill: {
                none: 'none'
            }
        },
    },
    plugins: [],
    purge: {
        content: [
            './resources/assets/index.html',
            './src/**/*.cljs',
            './src/**/*.cljc',
        ],
        extract: {
            // default: /[^<>"'`\s.(){}[\]#=%]*[^<>"'`\s.(){}[\]#=%:]/g
            // In CLJS classes are either
            // - hiccup style: :div.variant-a:class-a.variant-b:class-b
            // - keyword style: :variant-a:class-a
            // - string style: "variant-a:class-a variant-b:class-b"
            // The default fails to remove the leading colon from the keyword style
            DEFAULT: content => content.match(/[^<>"'`\s.(){}[\]#=%:][^<>"'`\s.(){}[\]#=%]*[^<>"'`\s.(){}[\]#=%:]/g) || []
        },
    },
};
