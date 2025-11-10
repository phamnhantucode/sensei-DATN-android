# Resume Builder Database Schema

Here is a general database schema based on your Kotlin models. This is a suggestion for how you could structure your tables.

### `resumes` table

This is the central table for a user's resume.

| Column Name           | Data Type      | Notes                                           |
| --------------------- | -------------- | ----------------------------------------------- |
| `id`                  | `TEXT`         | Primary Key, UUID                               |
| `user_id`             | `TEXT`         | Foreign Key to `users` table                    |
| `professional_summary`| `TEXT`         |                                                 |
| `theme_template_id`   | `TEXT`         | From `ResumeTheme`                              |
| `theme_primary_color` | `INTEGER`      | From `ColorScheme`                              |
| `theme_accent_color`  | `INTEGER`      | From `ColorScheme`                              |
| `theme_text_color`    | `INTEGER`      | From `ColorScheme`                              |
| `theme_background_color`| `INTEGER`      | From `ColorScheme`                              |
| `theme_section_header_color`| `INTEGER`| From `ColorScheme`                              |
| `theme_secondary_text_color`| `INTEGER`| From `ColorScheme`                              |
| `theme_font_family`   | `TEXT`         | From `TypographyScheme`                         |
| `theme_header_size`   | `REAL`         | From `TypographyScheme`                         |
| `theme_subheader_size`| `REAL`         | From `TypographyScheme`                         |
| `theme_body_size`     | `REAL`         | From `TypographyScheme`                         |
| `theme_caption_size`  | `REAL`         | From `TypographyScheme`                         |
| `theme_header_weight` | `INTEGER`      | From `TypographyScheme`                         |
| `theme_body_weight`   | `INTEGER`      | From `TypographyScheme`                         |
| `layout_type`         | `TEXT`         | From `LayoutConfig`                             |
| `layout_spacing`      | `INTEGER`      | From `LayoutConfig`                             |
| `layout_section_spacing`| `INTEGER`    | From `LayoutConfig`                             |
| `layout_section_style`| `TEXT`         | From `LayoutConfig`                             |

### `personal_info` table

Stores personal information for a resume.

| Column Name | Data Type | Notes                               |
| ----------- | --------- | ----------------------------------- |
| `id`        | `TEXT`    | Primary Key, UUID                   |
| `resume_id` | `TEXT`    | Foreign Key to `resumes` table      |
| `full_name` | `TEXT`    |                                     |
| `email`     | `TEXT`    |                                     |
| `phone`     | `TEXT`    |                                     |
| `location`  | `TEXT`    |                                     |
| `linked_in` | `TEXT`    |                                     |
| `portfolio` | `TEXT`    |                                     |
| `github`    | `TEXT`    |                                     |
| `avatar`    | `TEXT`    | URI or URL to profile picture       |

*Relationship: `resumes` has one `personal_info`.*

### `work_experiences` table

| Column Name      | Data Type | Notes                               |
| ---------------- | --------- | ----------------------------------- |
| `id`             | `TEXT`    | Primary Key, UUID                   |
| `resume_id`      | `TEXT`    | Foreign Key to `resumes` table      |
| `job_title`      | `TEXT`    |                                     |
| `company`        | `TEXT`    |                                     |
| `location`       | `TEXT`    |                                     |
| `start_date`     | `TEXT`    | ISO 8601 format (e.g., "YYYY-MM-DD") |
| `end_date`       | `TEXT`    | ISO 8601 format (e.g., "YYYY-MM-DD") |
| `is_current_role`| `INTEGER` | Boolean (0 or 1)                    |

*Relationship: `resumes` has many `work_experiences`.*

### `work_experience_responsibilities` table

Since responsibilities are a list of strings, we use a separate table.

| Column Name          | Data Type | Notes                                       |
| -------------------- | --------- | ------------------------------------------- |
| `id`                 | `INTEGER` | Primary Key, Auto-incrementing              |
| `work_experience_id` | `TEXT`    | Foreign Key to `work_experiences` table     |
| `responsibility`     | `TEXT`    |                                             |

*Relationship: `work_experiences` has many `work_experience_responsibilities`.*

### `education` table

| Column Name   | Data Type | Notes                               |
| ------------- | --------- | ----------------------------------- |
| `id`          | `TEXT`    | Primary Key, UUID                   |
| `resume_id`   | `TEXT`    | Foreign Key to `resumes` table      |
| `degree`      | `TEXT`    |                                     |
| `institution` | `TEXT`    |                                     |
| `location`    | `TEXT`    |                                     |
| `start_date`  | `TEXT`    | ISO 8601 format (e.g., "YYYY-MM-DD") |
| `end_date`    | `TEXT`    | ISO 8601 format (e.g., "YYYY-MM-DD") |
| `gpa`         | `TEXT`    |                                     |

*Relationship: `resumes` has many `education`.*

### `education_achievements` table

| Column Name   | Data Type | Notes                               |
| ------------- | --------- | ----------------------------------- |
| `id`          | `INTEGER` | Primary Key, Auto-incrementing      |
| `education_id`| `TEXT`    | Foreign Key to `education` table    |
| `achievement` | `TEXT`    |                                     |

*Relationship: `education` has many `education_achievements`.*

### `skills` table

| Column Name | Data Type | Notes                               |
| ----------- | --------- | ----------------------------------- |
| `id`        | `INTEGER` | Primary Key, Auto-incrementing      |
| `resume_id` | `TEXT`    | Foreign Key to `resumes` table      |
| `skill`     | `TEXT`    |                                     |

*Relationship: `resumes` has many `skills`.*

### `projects` table

| Column Name   | Data Type | Notes                               |
| ------------- | --------- | ----------------------------------- |
| `id`          | `TEXT`    | Primary Key, UUID                   |
| `resume_id`   | `TEXT`    | Foreign Key to `resumes` table      |
| `title`       | `TEXT`    |                                     |
| `description` | `TEXT`    |                                     |
| `link`        | `TEXT`    |                                     |
| `start_date`  | `TEXT`    | ISO 8601 format (e.g., "YYYY-MM-DD") |
| `end_date`    | `TEXT`    | ISO 8601 format (e.g., "YYYY-MM-DD") |

*Relationship: `resumes` has many `projects`.*

### `project_technologies` table

| Column Name | Data Type | Notes                               |
| ----------- | --------- | ----------------------------------- |
| `id`        | `INTEGER` | Primary Key, Auto-incrementing      |
| `project_id`| `TEXT`    | Foreign Key to `projects` table     |
| `technology`| `TEXT`    |                                     |

*Relationship: `projects` has many `project_technologies`.*

### `certifications` table

| Column Name    | Data Type | Notes                               |
| -------------- | --------- | ----------------------------------- |
| `id`           | `TEXT`    | Primary Key, UUID                   |
| `resume_id`    | `TEXT`    | Foreign Key to `resumes` table      |
| `name`         | `TEXT`    |                                     |
| `issuer`       | `TEXT`    |                                     |
| `issue_date`   | `TEXT`    | ISO 8601 format (e.g., "YYYY-MM-DD") |
| `expiry_date`  | `TEXT`    | ISO 8601 format (e.g., "YYYY-MM-DD") |
| `credential_id`| `TEXT`    |                                     |

*Relationship: `resumes` has many `certifications`.*

### `languages` table

| Column Name   | Data Type | Notes                               |
| ------------- | --------- | ----------------------------------- |
| `id`          | `TEXT`    | Primary Key, UUID                   |
| `resume_id`   | `TEXT`    | Foreign Key to `resumes` table      |
| `name`        | `TEXT`    |                                     |
| `proficiency` | `TEXT`    | From `LanguageProficiency` enum     |

*Relationship: `resumes` has many `languages`.*
