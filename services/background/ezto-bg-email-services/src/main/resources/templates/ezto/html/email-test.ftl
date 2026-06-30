<html>
<body>
${kcSanitize(msg("emailTestBodyHtml",realm_name)?replace("QlikVerify", variant))?no_esc}
</body>
</html>