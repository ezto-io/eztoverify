const { body, param, validationResult } = require('express-validator');

const validateGeneratesShotenedUrlRequest = [
  body('url')
  .notEmpty().withMessage("url is mandatory"),
  body('ttl')
  .notEmpty().withMessage("ttl is mandatory")
];

const validateGetShotenedUrlRequest = [
    param('id')
    .notEmpty().withMessage("id is mandatory"),
];

module.exports = {
    validateGeneratesShotenedUrlRequest,
    validateGetShotenedUrlRequest
};