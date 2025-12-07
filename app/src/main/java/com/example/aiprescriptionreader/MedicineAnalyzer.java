package com.example.aiprescriptionreader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MedicineAnalyzer {

    public static String analyzePrescription(String text) {
        if (text == null || text.isEmpty()) {
            return "No text to analyze";
        }

        StringBuilder result = new StringBuilder();
        List<Medicine> medicines = findMedicines(text);

        if (medicines.isEmpty()) {
            result.append("⚠️ No specific medicines detected.\n");
            result.append("Try scanning a clearer prescription.\n\n");
        } else {
            result.append("✅ Found ").append(medicines.size()).append(" medicine(s):\n\n");
            for (Medicine medicine : medicines) {
                result.append(medicine).append("\n");
            }
        }

        result.append("📋 Summary:\n");
        result.append(getSummary(text));

        return result.toString();
    }

    private static List<Medicine> findMedicines(String text) {
        List<Medicine> medicines = new ArrayList<>();
        String[] lines = text.split("\n");

        String[] commonMeds = {
                "paracetamol", "ibuprofen", "aspirin", "amoxicillin",
                "azithromycin", "metformin", "insulin", "atorvastatin",
                "losartan", "amlodipine", "omeprazole", "levothyroxine"
        };

        for (String line : lines) {
            String lowerLine = line.toLowerCase();

            for (String med : commonMeds) {
                if (lowerLine.contains(med)) {
                    String name = extractMedicineName(line);
                    String dosage = findDosage(line);
                    String frequency = findFrequency(line);
                    String duration = findDuration(line);

                    medicines.add(new Medicine(name, dosage, frequency, duration));
                    break;
                }
            }
        }

        return medicines;
    }

    private static String extractMedicineName(String line) {
        String[] words = line.split("\\s+");
        if (words.length > 0) {
            return words[0];
        }
        return "Medicine";
    }

    private static String findDosage(String text) {
        Pattern pattern = Pattern.compile("(\\d+\\s*(mg|g|ml|tablet|tab)s?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "Not specified";
    }

    private static String findFrequency(String text) {
        Pattern pattern = Pattern.compile("(\\d+ times? (a|per) day|once daily|twice daily)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "Not specified";
    }

    private static String findDuration(String text) {
        Pattern pattern = Pattern.compile("(for \\d+ days?|\\d+ days? course)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "Not specified";
    }

    private static String getSummary(String text) {
        StringBuilder summary = new StringBuilder();
        String lowerText = text.toLowerCase();

        if (lowerText.contains("tablet") || lowerText.contains("tab")) {
            summary.append("• Oral tablets\n");
        }
        if (lowerText.contains("capsule") || lowerText.contains("cap")) {
            summary.append("• Capsules\n");
        }
        if (lowerText.contains("syrup") || lowerText.contains("suspension")) {
            summary.append("• Liquid medicine\n");
        }
        if (lowerText.contains("injection") || lowerText.contains("inj")) {
            summary.append("• Injections\n");
        }
        if (lowerText.contains("ointment") || lowerText.contains("cream")) {
            summary.append("• Topical application\n");
        }

        if (summary.length() == 0) {
            summary.append("• General prescription\n");
        }

        return summary.toString();
    }

    static class Medicine {
        String name, dosage, frequency, duration;

        Medicine(String name, String dosage, String frequency, String duration) {
            this.name = name;
            this.dosage = dosage;
            this.frequency = frequency;
            this.duration = duration;
        }

        @Override
        public String toString() {
            return String.format("💊 %s\n   Dose: %s\n   Frequency: %s\n   Duration: %s\n",
                    name, dosage, frequency, duration);
        }
    }
}