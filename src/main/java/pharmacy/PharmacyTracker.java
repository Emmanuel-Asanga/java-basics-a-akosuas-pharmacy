package pharmacy;

import pharmacy.ui.ConsoleView;

/** Entry point: wires the console view to the demo scenarios. */
public class PharmacyTracker {

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        PharmacyDemo.run(view);
        PharmacyDemo.runReorderDemo(view);
    }
}
