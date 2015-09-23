/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.component;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.concat;
import static java.util.Objects.requireNonNull;

/**
 * This crawler make any number of {@link TypeAwareVisitor} or {@link PathAwareVisitor} defined in a list visit a component tree, component per component, in the order of the list
 */
public class VisitorsCrawler implements ComponentCrawler {

  private static final Logger LOGGER = Loggers.get(VisitorsCrawler.class);

  private final List<VisitorWrapper> preOrderVisitorWrappers;
  private final List<VisitorWrapper> postOrderVisitorWrappers;

  public VisitorsCrawler(Iterable<ComponentVisitor> visitors) {
    List<VisitorWrapper> visitorWrappers = from(visitors).transform(ToVisitorWrapper.INSTANCE).toList();
    this.preOrderVisitorWrappers = from(visitorWrappers).filter(MathPreOrderVisitor.INSTANCE).toList();
    this.postOrderVisitorWrappers = from(visitorWrappers).filter(MatchPostOrderVisitor.INSTANCE).toList();
  }

  @Override
  public void visit(final Component component) {
    MatchVisitorMaxDepth visitorMaxDepth = MatchVisitorMaxDepth.forComponent(component);
    List<VisitorWrapper> preOrderVisitorWrappersToExecute = from(preOrderVisitorWrappers).filter(visitorMaxDepth).toList();
    List<VisitorWrapper> postOrderVisitorWrappersToExecute = from(postOrderVisitorWrappers).filter(visitorMaxDepth).toList();
    if (preOrderVisitorWrappersToExecute.isEmpty() && postOrderVisitorWrappersToExecute.isEmpty()) {
      return;
    }

    for (VisitorWrapper visitorWrapper : concat(preOrderVisitorWrappers, postOrderVisitorWrappers)) {
      visitorWrapper.beforeComponent(component);
    }

    for (VisitorWrapper visitorWrapper : preOrderVisitorWrappersToExecute) {
      visitNode(component, visitorWrapper);
    }

    visitChildren(component);

    for (VisitorWrapper visitorWrapper : postOrderVisitorWrappersToExecute) {
      visitNode(component, visitorWrapper);
    }

    for (VisitorWrapper visitorWrapper : concat(preOrderVisitorWrappersToExecute, postOrderVisitorWrappersToExecute)) {
      visitorWrapper.afterComponent(component);
    }
  }

  private void visitChildren(Component component) {
    for (Component child : component.getChildren()) {
      visit(child);
    }
  }

  private void visitNode(Component component, VisitorWrapper visitor) {
    LOGGER.trace("Visitor '{}' is currently visiting component {}", visitor, component);
    visitor.visitAny(component);
    switch (component.getType()) {
      case PROJECT:
        visitor.visitProject(component);
        break;
      case MODULE:
        visitor.visitModule(component);
        break;
      case DIRECTORY:
        visitor.visitDirectory(component);
        break;
      case FILE:
        visitor.visitFile(component);
        break;
      case VIEW:
        visitor.visitView(component);
        break;
      case SUBVIEW:
        visitor.visitSubView(component);
        break;
      case PROJECT_VIEW:
        visitor.visitProjectView(component);
        break;
      default:
        throw new IllegalStateException(String.format("Unknown type %s", component.getType().name()));
    }
  }

  private enum ToVisitorWrapper implements Function<ComponentVisitor, VisitorWrapper> {
    INSTANCE;

    @Override
    public VisitorWrapper apply(@Nonnull ComponentVisitor componentVisitor) {
      if (componentVisitor instanceof TypeAwareVisitor) {
        return new TypeAwareVisitorWrapper((TypeAwareVisitor) componentVisitor);
      } else if (componentVisitor instanceof PathAwareVisitor) {
        return new PathAwareVisitorWrapper((PathAwareVisitor) componentVisitor);
      } else {
        throw new IllegalArgumentException("Only TypeAwareVisitor and PathAwareVisitor can be used");
      }
    }
  }

  private static class MatchVisitorMaxDepth implements Predicate<VisitorWrapper> {
    private static final Map<Component.Type, MatchVisitorMaxDepth> INSTANCES = buildInstances();

    private static Map<Component.Type, MatchVisitorMaxDepth> buildInstances() {
      ImmutableMap.Builder<Component.Type, MatchVisitorMaxDepth> builder = ImmutableMap.builder();
      for (Component.Type type : Component.Type.values()) {
        builder.put(type, new MatchVisitorMaxDepth(type));
      }
      return builder.build();
    }

    private final Component.Type type;

    private MatchVisitorMaxDepth(Component.Type type) {
      this.type = requireNonNull(type);
    }

    public static MatchVisitorMaxDepth forComponent(Component component) {
      return INSTANCES.get(component.getType());
    }

    @Override
    public boolean apply(@Nonnull VisitorWrapper visitorWrapper) {
      CrawlerDepthLimit maxDepth = visitorWrapper.getMaxDepth();
      return maxDepth.isSameAs(type) || maxDepth.isDeeperThan(type);
    }
  }

  private enum MathPreOrderVisitor implements Predicate<VisitorWrapper> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull VisitorWrapper visitorWrapper) {
      return visitorWrapper.getOrder() == ComponentVisitor.Order.PRE_ORDER;
    }
  }

  private enum MatchPostOrderVisitor implements Predicate<VisitorWrapper> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull VisitorWrapper visitorWrapper) {
      return visitorWrapper.getOrder() == ComponentVisitor.Order.POST_ORDER;
    }
  }
}
