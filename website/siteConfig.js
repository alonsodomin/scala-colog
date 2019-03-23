/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// See https://docusaurus.io/docs/site-config for all the possible
// site configuration options.

const repoUrl = "https://github.com/alonsodomin/scala-colog";
const apiUrl = "https://alonsodomin.github.io/scala-colog/api";

// List of projects/orgs using your project for the users page.
const users = [];
/*const users = [
  {
    caption: 'User1',
    // You will need to prepend the image path with your baseUrl
    // if it is not '/', like: '/test-site/img/docusaurus.svg'.
    image: '/img/docusaurus.svg',
    infoLink: 'https://www.facebook.com',
    pinned: true,
  },
];*/

const siteConfig = {
  title: 'Scala Colog',
  tagline: 'A pure functional programming logging framework for Scala',
  url: 'https://alonsodomin.github.io',
  baseUrl: '/scala-colog/',

  customDocsPath: 'website/target/mdoc',

  // Used for publishing and more
  projectName: 'scala-colog',
  organizationName: 'alonsodomin',
  // For top-level user or org sites, the organization is still the same.
  // e.g., for the https://JoelMarcey.github.io site, it would be set like...
  //   organizationName: 'JoelMarcey'

  // For no header links in the top nav bar -> headerLinks: [],
  headerLinks: [
    {doc: 'overview', label: 'Documentation'},
    {href: apiUrl, label: 'API Docs', external: true},
    {href: repoUrl, label: "GitHub", external: true},
  ],

  // If you have users set above, you add it here:
  users,

  /* path to images for header/footer */
  headerIcon: 'img/logs.white.svg',
  footerIcon: 'img/logs.white.svg',
  favicon: 'img/favicon.png',

  /* Colors for website */
  colors: {
    primaryColor: '#5487bd',
    secondaryColor: '#3e507d',
  },

  /* Custom fonts for website */
  /*
  fonts: {
    myFont: [
      "Times New Roman",
      "Serif"
    ],
    myOtherFont: [
      "-apple-system",
      "system-ui"
    ]
  },
  */

  // This copyright info is used in /core/Footer.js and blog RSS/Atom feeds.
  copyright: `Copyright © ${new Date().getFullYear()} A. Alonso Dominguez`,

  highlight: {
    // Highlight.js theme to use for syntax highlighting in code blocks.
    theme: 'github',
  },

  // Add custom scripts here that would be placed in <script> tags.
  scripts: ['https://buttons.github.io/buttons.js'],

  // On page navigation for the current documentation page.
  onPageNav: 'separate',
  // No .html extensions for paths.
  cleanUrl: true,

  // Open Graph and Twitter card images.
  ogImage: 'img/logs.png',
  twitterImage: 'img/logs.png',

  // Show documentation's last contributor's name.
  // enableUpdateBy: true,

  // Show documentation's last update time.
  enableUpdateTime: true,

  editUrl: `${repoUrl}/edit/master/docs/`,

  apiUrl,
  repoUrl,
};

module.exports = siteConfig;
