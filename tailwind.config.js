module.exports = {
    theme: {
        width: (theme) => ({
            auto: 'auto',
            ...theme('spacing'),
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
            full: '100%',
            screen: '100vw',
        }),
        translate: (theme, { negative }) => ({
            ...theme('spacing'),
            ...negative(theme('spacing')),
            '-full': '-100%',
            // originally 1/2
            '-1|2': '-50%',
            '1|2': '50%',
            full: '100%',
        }),
        extend: {
            maxWidth: {
                screen: '100vw',
            },
            fill: {
                none: 'none'
            }
        },
    },
    variants: {
        textColor: ['responsive', 'hover', 'focus', 'group-hover'],
        textDecoration: ['responsive', 'hover', 'focus', 'group-hover'],
    },
    plugins: [
    ],
    purge: {
        content: [
            './assets/index.html',
            './src/**/*.cljs',
            './src/**/*.cljc',
        ],
        options: {
            // Include any special characters you're using in this regular expression
            defaultExtractor: content => content.match(/[\w-][\w-:|]*/g) || []
        },
    },
};
