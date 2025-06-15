package com.example.myapplication

object Constants {
    // Request Codes
    const val REQUEST_CODE_CLIENT_SELECTION = 1
    const val REQUEST_CODE_ADD_NEW_CLIENT = 2
    const val REQUEST_CODE_ADD_NEW_ARTICLE = 3
    const val REQUEST_CODE_IMAGE_CAPTURE = 4
    const val REQUEST_CODE_IMAGE_GALLERY = 5
    const val REQUEST_CODE_SCAN_BARCODE = 6
    const val REQUEST_CODE_PERMISSIONS = 7
    const val REQUEST_CODE_READ_STORAGE = 8
    const val REQUEST_CODE_WRITE_STORAGE = 9
    const val REQUEST_CODE_OPEN_DOCUMENT_TREE = 10
    const val REQUEST_CODE_PICK_BACKUP_FILE = 11
    const val REQUEST_CODE_ADD_NEW_INVOICE = 12 // Adicionado para clareza em MainActivity

    // SharedPreferences Keys
    const val PREFS_NAME = "MyPrefs"
    const val LAST_INVOICE_NUMBER_KEY = "last_invoice_number"
    const val LOGO_PATH_KEY = "logotipo_path"
    const val COMPANY_INFO_NAME_KEY = "company_name"
    const val COMPANY_INFO_ADDRESS_KEY = "company_address"
    const val COMPANY_INFO_PHONE_KEY = "company_phone"
    const val COMPANY_INFO_EMAIL_KEY = "company_email"
    const val COMPANY_INFO_CNPJ_KEY = "company_cnpj"
    const val PAYMENT_INSTRUCTIONS_KEY = "payment_instructions"
    const val DEFAULT_NOTES_KEY = "default_notes"

    // File/Directory Names
    const val APP_DIR_NAME = "MyApplicationFaturas"
    const val INVOICES_DIR_NAME = "Faturas"
    const val IMAGES_DIR_NAME = "Imagens"
    const val DATABASE_NAME = "clientes.db"
    const val BACKUP_ZIP_NAME = "backup_myapplication.zip"
    const val LOGO_FILE_NAME = "logo.png"

    // API
    const val CNPJ_API_BASE_URL = "https://www.receitaws.com.br/v1/cnpj/"

    // Database
    const val DATABASE_VERSION = 12

    // Other Constants
    const val ARTIGO_SELECTION_LIMIT = 20
    const val CLIENTE_RECENT_LIMIT = 5
    const val DEFAULT_INVOICE_PREFIX = "FAT"
}