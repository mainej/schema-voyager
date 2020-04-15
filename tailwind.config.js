const _ = require('lodash');
const plugin = require('tailwindcss/plugin');

module.exports = {
    theme: {
        width: (theme) => ({
            auto: 'auto',
            ...theme('spacing'),
            // originally 1/2, etc
            '1of2': '50%',
            '1of3': '33.333333%',
            '2of3': '66.666667%',
            '1of4': '25%',
            '2of4': '50%',
            '3of4': '75%',
            '1of5': '20%',
            '2of5': '40%',
            '3of5': '60%',
            '4of5': '80%',
            '1of6': '16.666667%',
            '2of6': '33.333333%',
            '3of6': '50%',
            '4of6': '66.666667%',
            '5of6': '83.333333%',
            '1of12': '8.333333%',
            '2of12': '16.666667%',
            '3of12': '25%',
            '4of12': '33.333333%',
            '5of12': '41.666667%',
            '6of12': '50%',
            '7of12': '58.333333%',
            '8of12': '66.666667%',
            '9of12': '75%',
            '10of12': '83.333333%',
            '11of12': '91.666667%',
            full: '100%',
            screen: '100vw',
        }),
        translate: (theme, { negative }) => ({
            ...theme('spacing'),
            ...negative(theme('spacing')),
            '-full': '-100%',
            // originally 1/2
            '-1of2': '-50%',
            '1of2': '50%',
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
        /* https://every-layout.dev/layouts/stack/ */
        plugin(function ({ addUtilities, e, theme, variants }) {
            const utilities = _.flatMap(theme('padding'), (size, modifier) => ({
                [`.${e(`stack-my-${modifier}`)} > * + *`]: { marginTop: `${size}` },
                [`.${e(`stack-mx-${modifier}`)} > * + *`]: { marginLeft: `${size}` },
                [`.${e(`stack-py-${modifier}`)} > * + *`]: { paddingTop: `${size}` },
                [`.${e(`stack-px-${modifier}`)} > * + *`]: { paddingLeft: `${size}` },
            }));
            addUtilities(utilities, variants('padding'));
        }),
        plugin(function ({ addUtilities, e, theme, variants }) {
            const generator = (value, modifier) => ({
                [`.${e(`stack-border-y${modifier}`)} > * + *`]: { borderTopWidth: `${value}` },
                [`.${e(`stack-border-x${modifier}`)} > * + *`]: { borderLeftWidth: `${value}` },
            });
            const utilities = _.flatMap(theme('borderWidth'), (value, modifier) => {
                return generator(value, modifier === 'default' ? '' : `-${modifier}`);
            });
            addUtilities(utilities, variants('borderWidth'));
        }),
    ],
};
