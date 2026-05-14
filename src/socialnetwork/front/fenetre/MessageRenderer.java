package socialnetwork.front.fenetre;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import socialnetwork.back.model.MessageEvent;

/**
 * Renderer de liste pour les {@link MessageEvent}.
 * Colorie les lignes selon la bienveillance du message.
 *
 * <pre>
 *   >= +5  → vert vif       (tres positif)
 *   >= +1  → vert pale      (positif)
 *   >= -1  → blanc          (neutre)
 *   >= -5  → orange pale    (negatif)
 *   <  -5  → rouge pale     (tres negatif)
 * </pre>
 *
 * Extrait de {@code FenetreBavard} pour pouvoir etre reutilise dans
 * d'autres composants de l'interface.
 */
public final class MessageRenderer extends DefaultListCellRenderer {

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (value instanceof MessageEvent && !isSelected) {
			float b = ((MessageEvent) value).getBienveillance();
			if (b >= 5f)       setBackground(new Color(100, 210, 100));
			else if (b >= 1f)  setBackground(new Color(200, 240, 200));
			else if (b >= -1f) setBackground(Color.WHITE);
			else if (b >= -5f) setBackground(new Color(255, 230, 150));
			else               setBackground(new Color(255, 180, 180));
		}
		return this;
	}
}
